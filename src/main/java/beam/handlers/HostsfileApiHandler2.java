package beam.handlers;

import beam.BeamException;
import beam.BeamRuntime;
import beam.cli.PrimaryCommand;
import beam.cli.ServerCommand;
import beam.config.RootConfig;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.psddev.dari.util.ObjectUtils;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class HostsfileApiHandler2 implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HostsfileApiHandler2.class);

    private static final Map<String, ReplicationData> REPLICATIONS = new ConcurrentHashMap<>();

    private static final Executor executor = Executors.newFixedThreadPool(4);

    private static final Executor replicationExecutor = Executors.newFixedThreadPool(100);

    private static int BATCH_INTERVAL = 5000;

    private static long WAIT = -1;

    private static long nextReplication = 0;

    private static final LoadingCache<PrimaryCacheKey, String> primaryCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .refreshAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<PrimaryCacheKey, String>() {
                @Override
                public String load(PrimaryCacheKey key) throws Exception {
                    BeamRuntime envRuntime = BeamRuntime.getRuntimeForEnvironment(key.env);

                    String primary = envRuntime.getStorage().getString(key.key, key.region);
                    return primary == null ? "" : primary;
                }

                @Override
                public ListenableFuture<String> reload(final PrimaryCacheKey primaryKey, final String oldValue) throws Exception {
                    ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
                        public String call() throws Exception {
                            Thread.currentThread().setName("primary-cache-loader");

                            String newValue = load(primaryKey);
                            if (!oldValue.equals(newValue)) {
                                LOGGER.info(String.format("Pinned primary changed %s: %s => %s", primaryKey.key, oldValue, newValue));
                            }

                            return newValue;
                        }
                    });
                    executor.execute(task);
                    return task;
                }
            });

    private boolean debug;
    private String region;
    private String serverDNS;

    public HostsfileApiHandler2(boolean debug, String region, int batchInterval, String serverDNS) {
        this.debug = debug;
        this.region = region;
        this.serverDNS = serverDNS;

        if (batchInterval > 0) {
            BATCH_INTERVAL = batchInterval;
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();

        Map<String, Deque<String>> params = exchange.getQueryParameters();

        String instanceId = params.get("instanceId") != null ? params.get("instanceId").element() : null;
        String environment = params.get("environment") != null ? params.get("environment").element() : null;
        String layer = params.get("layer") != null ? params.get("layer").element() : null;
        String location = params.get("location") != null ? params.get("location").element() : null;
        String ip = params.get("ip") != null ? params.get("ip").element() : null;
        String launchedString = params.get("launched") != null ? params.get("launched").element() : null;
        String primariesByServiceString = params.get("primariesByService") != null ? params.get("primariesByService").element() : null;
        String serviceChecksString = params.get("serviceChecks") != null ? params.get("serviceChecks").element() : null;
        boolean buildHostsFile = params.get("replication") == null;

        File stateFile = new File(ServiceState.INSTANCE_STATE_FILE);
        DumperOptions options = new DumperOptions();
        options.setWidth(80);
        options.setIndent(4);

        Yaml yaml = new Yaml(options);

        if (ServiceState.getServicePrimaries().isEmpty()) {
            SetPrimaryHandler.load();
        }

        // Remove any instances that haven't sent a heartbeat message recently.
        for (String id : ServiceState.getInstances().keySet()) {
            ServiceInstance instance = ServiceState.getServiceInstance(id);
            Set<String> pinnedServices = ServiceState.getPinnedServicesForInstance(id);

            if (instance.getLastPing() < System.currentTimeMillis() - (86400 * 1000) && pinnedServices.isEmpty()) {
                // Instances that haven't been heard from in over 24 hrs without pinned primaries get completely removed.
                    ServiceState.removeInstance(id);
                    ServiceState.removeInstanceMark(id);
            } else if (!instance.isOk()) {
                // Instances that haven't been heard from in over 60 seconds have their
                // services that don't have a pinned primary marked failed with status 99.
                // This way they still show up in DNS but no longer participate.
                for (ServiceInstance.Service service : instance.getServiceInfo()) {
                    if (!pinnedServices.contains(service.getName())) {
                        service.setStatus(99);
                    }
                }
            }
        }

        // Read instance data from disk if there are no cached instances. This allows the
        // server to be updated without losing state.
        if (ServiceState.getInstances().isEmpty() && stateFile.exists()) {
            try {
                Map<String, ServiceInstance> instances = (Map<String, ServiceInstance>) yaml.load(new FileInputStream(stateFile));

                // Reset heartbeats one time to give instances a chance to heartbeat again
                // before they are removed.
                for (ServiceInstance serviceInstance : instances.values()) {
                    serviceInstance.setLastPing(System.currentTimeMillis());
                }

                ServiceState.putAllInstances(instances);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else if (ServiceState.getInstances().isEmpty() && !stateFile.exists()) {
            WAIT = System.currentTimeMillis() + ServerCommand.serviceCooldown;
        }

        // Save the new state.
        FileWriter filewriter = new FileWriter(stateFile);
        try {
            ServiceState.writeInstances(yaml, filewriter);
        } finally {
            filewriter.close();
        }

        // Save the primary states
        SetPrimaryHandler.save();

        ServiceInstance instance = null;
        if (instanceId != null) {
            Preconditions.checkNotNull(environment, "environment");
            Preconditions.checkNotNull(layer, "layer");
            Preconditions.checkNotNull(location, "location");
            Preconditions.checkNotNull(ip, "ip");
            Preconditions.checkNotNull(launchedString, "launchedString");

            instance = new ServiceInstance();

            instance.setId(instanceId);
            instance.setEnvironment(environment);
            instance.setLayer(layer);
            instance.setLocation(location);
            instance.setPrivateIpAddress(ip);
            instance.setLaunchTime(0L);
            instance.setLastPing(System.currentTimeMillis());

            if (ServiceState.getInstanceMarks().containsKey(instanceId)) {
                instance.setMark(ServiceState.getMark(instanceId));
            } else {
                instance.setMark(null);
            }

            if (launchedString != null) {
                instance.setLaunchTime(Long.valueOf(launchedString));
            }

            Map<String, List<String>> primariesByService = (Map<String, List<String>>) ObjectUtils.fromJson(primariesByServiceString);
            for (String serviceName : primariesByService.keySet()) {
                List<String> primaries = primariesByService.get(serviceName);

                ServiceInstance.Service service = new ServiceInstance.Service();
                service.setName(serviceName);
                service.setPrimaries(new HashSet<>(primaries));

                instance.addServiceInfo(service);
            }

            for (Map.Entry<String, Number> entry : ((Map<String, Number>) ObjectUtils.fromJson(serviceChecksString)).entrySet()) {
                if (entry.getValue() != null) {
                    ServiceInstance.Service service = instance.getServiceInfo(entry.getKey());
                    if (service == null) {
                        service = new ServiceInstance.Service();
                    }

                    service.setName(entry.getKey());
                    service.setStatus(entry.getValue().intValue());

                    instance.addServiceInfo(service);
                }
            }

            ServiceState.setServiceInstance(instanceId, instance);
        }

        if (buildHostsFile) {
            if (WAIT < System.currentTimeMillis()) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter out = new PrintWriter(stringWriter);

                try {
                    buildHostsfile(instance, out);

                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
                    exchange.getResponseSender().send(stringWriter.toString());
                    exchange.endExchange();
                } catch (Exception ex) {
                    LOGGER.error("Hostsfile request failed.", ex);
                    exchange.setStatusCode(500);
                    return;
                }

            } else {
                LOGGER.info("Hostsfile is not ready.");
                exchange.setStatusCode(204);
            }

            if (instance != null) {
                replicateServices(instance, primariesByServiceString, serviceChecksString);
            }
        }
    }

    private void buildHostsfile(ServiceInstance requestingInstance, PrintWriter out) throws Exception {
        Map<String, List<ServiceInstance>> environmentInstances = new HashMap<>();

        if (ServiceState.getInstances().isEmpty()) {
            throw new BeamException("No instances registered.");
        }

        for (ServiceInstance serviceInstance : ServiceState.getServiceInstances()) {
            String env = serviceInstance.getEnvironment();
            List<ServiceInstance> envInstances = environmentInstances.get(env);
            if (envInstances == null) {
                envInstances = new ArrayList<>();
                environmentInstances.put(env, envInstances);
            }

            envInstances.add(serviceInstance);
        }

        for (String env : environmentInstances.keySet()) {
            List<ServiceInstance> instances = environmentInstances.get(env);
            out.write("# " + env + "\n");
            buildHostsfileForEnv(env, requestingInstance, instances, out);
            out.write("\n");
        }
    }

    private void buildHostsfileForEnv(String env, ServiceInstance requestingInstance, List<ServiceInstance> instances, PrintWriter out) throws Exception {
        Map<String, List<ServiceInstance>> serviceMap = new HashMap<>();
        Map<String, List<ServiceInstance>> regionServiceMap = new HashMap<>();
        Map<String, Object> primaryMap = new HashMap<>();

        // Map service names to near by instances that provide the service.
        Set<String> services = new HashSet<>();
        for (ServiceInstance serviceInstance : instances) {
            for (ServiceInstance.Service service : serviceInstance.getServiceInfo()) {
                String serviceName = service.getName();
                services.add(serviceName);

                if ((!serviceInstance.isOk() || !service.isOk())) {
                    continue;
                }

                // Map service:primary names to instances that provide those services.
                for (String primaryName : service.getPrimaries()) {
                    String primaryKey = String.format("%s:%s", serviceName, primaryName);

                    List<ServiceInstance> primaryInstances = (List<ServiceInstance>) primaryMap.get(primaryKey);
                    if (primaryInstances == null) {
                        primaryInstances = new ArrayList<>();
                        primaryMap.put(primaryKey, primaryInstances);
                    }

                    primaryInstances.add(serviceInstance);
                }

                // serviceMap is a map of instances in the same location as the requesting instance.
                if (requestingInstance == null || serviceInstance.isInLocation(requestingInstance.getLocation())) {
                    List<ServiceInstance> serviceInstances = serviceMap.get(serviceName);
                    if (serviceInstances == null) {
                        serviceInstances = new ArrayList<>();
                        serviceMap.put(serviceName, serviceInstances);
                    }

                    serviceInstances.add(serviceInstance);
                }

                // regionServiceMap is a map of instances nearby the requesting instance.
                if (requestingInstance == null || serviceInstance.isNearLocation(requestingInstance.getLocation())) {
                    List<ServiceInstance> serviceInstances = regionServiceMap.get(serviceName);
                    if (serviceInstances == null) {
                        serviceInstances = new ArrayList<>();
                        regionServiceMap.put(serviceName, serviceInstances);
                    }

                    serviceInstances.add(serviceInstance);
                }
            }
        }

        for (String serviceName : services) {
            // If no instances are in the same location as the requesting instance then
            // fall back to nearby instances.
            List<ServiceInstance> serviceInstances = serviceMap.get(serviceName);
            if (serviceInstances == null) {
                serviceInstances = regionServiceMap.get(serviceName);
                serviceMap.put(serviceName, serviceInstances);
            }
        }

        // Sort service:primary instances by age, oldest first.
        for (String primaryKey : primaryMap.keySet()) {
            List<ServiceInstance> serviceInstances = (List<ServiceInstance>) primaryMap.get(primaryKey);

            // Oldest first.
            Collections.sort(serviceInstances, new Comparator<ServiceInstance>() {
                @Override
                public int compare(ServiceInstance o1, ServiceInstance o2) {
                    if (o1.getLaunchTime().equals(o2.getLaunchTime())) {
                        return o1.getId().compareTo(o2.getId());
                    } else if (o1.getLaunchTime() < o2.getLaunchTime()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
        }

        // Are any of the primaries set statically?
        for (Map.Entry<String, Object> entry : primaryMap.entrySet()) {
            String key = entry.getKey();
            String service = key.split(":")[0];
            String primary = key.split(":")[1];
            String override = null;

            BeamRuntime envRuntime = BeamRuntime.getRuntimeForEnvironment(env);

            // Try region specific path first, falling back to global.
            String regionKey = PrimaryCommand.getStorageKey(envRuntime, service, primary, requestingInstance.getRegion());
            String globalKey = PrimaryCommand.getStorageKey(envRuntime, service, primary);

            if (ServiceState.getServicePrimaries().containsKey(regionKey)) {
                override = ServiceState.getPrimaryData(regionKey).getInstanceId();
            }

            if (ObjectUtils.isBlank(override)) {
                if (ServiceState.getServicePrimaries().containsKey(globalKey)) {
                    override = ServiceState.getPrimaryData(globalKey).getInstanceId();
                }
            }

            // Fallback to s3
            if (override == null) {
                try {
                    // Try region specific path first, falling back to global.
                    PrimaryCacheKey s3RegionKey = new PrimaryCacheKey(env, region, PrimaryCommand.getStorageKey(envRuntime, service, primary, requestingInstance.getRegion()));
                    override = primaryCache.get(s3RegionKey);

                    if (!ObjectUtils.isBlank(override)) {
                        ServiceState.setPrimaryData(regionKey, new ServiceState.PrimaryData(override, 0));

                    } else {
                        PrimaryCacheKey s3GlobalKey = new PrimaryCacheKey(env, region, PrimaryCommand.getStorageKey(envRuntime, service, primary));
                        override = primaryCache.get(s3GlobalKey);
                        if (!ObjectUtils.isBlank(override)) {
                            ServiceState.setPrimaryData(globalKey, new ServiceState.PrimaryData(override, 0));
                        }
                    }
                } catch (Exception error) {

                }
            }

            if (!ObjectUtils.isBlank(override)) {
                entry.setValue(override);
            }
        }

        // For each service pick one instance for this particular host.
        Map<String, ServiceInstance> zoneServiceMap = new HashMap<>();
        if (requestingInstance.getId() != null) {
            for (String service : serviceMap.keySet()) {
                int index = 0;

                // Calculate the index of the host requesting a hostsfile in its environment.
                for (ServiceInstance i : instances) {
                    if (i.getLocation().equals(requestingInstance.getLocation()) &&
                            i.getLayer().equals(requestingInstance.getLayer()) && i.isOk()) {
                        if (i == requestingInstance) {
                            break;
                        }

                        index++;
                    }
                }

                List<ServiceInstance> serviceInstances = serviceMap.get(service);
                if (serviceInstances != null && serviceInstances.size() > 0) {
                    zoneServiceMap.put(service, pickInstance(index, serviceMap.get(service)));
                }
            }
        }

        for (ServiceInstance i : instances) {
            List<String> instanceHostnames = new ArrayList<>();

            instanceHostnames.addAll(getInstanceHostnames(i, requestingInstance.getLocation(), zoneServiceMap, primaryMap));
            String hostname = instanceHostnames.get(0);
            instanceHostnames.remove(0);

            Collections.sort(instanceHostnames);
            instanceHostnames.add(0, hostname);
            instanceHostnames.add(0, i.getId());
            instanceHostnames.add(0, i.getPrivateIpAddress());

            out.write(StringUtils.join(instanceHostnames, " "));
            out.write("\n\n");
            out.flush();
        }
    }

    /**
     * Pick an single service instance for a particule index. This will load balance backend
     * services across the frontend layer.
     */
    private ServiceInstance pickInstance(int index, final List<ServiceInstance> instances) {
        return instances.get(index % instances.size());
    }

    /**
     * Return a list of applicable hostnames for a given instance.
     */
    private List<String> getInstanceHostnames(ServiceInstance instance,
                                              String location,
                                              Map<String, ServiceInstance> zoneServiceMap,
                                              Map<String, Object> primaryMap) {
        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();

        List<String> hostnames = new ArrayList<>();

        String fullDomain =  instance.getEnvironment() + "." + runtime.getInternalDomain();

        hostnames.add(instance.getId() + "." + instance.getLayer() + ".layer." + fullDomain);
        hostnames.add(instance.getId() + "." + fullDomain);

        // Zone level hostname.
        for (String serviceName : zoneServiceMap.keySet()) {
            ServiceInstance serviceInstance = zoneServiceMap.get(serviceName);
            ServiceInstance.Service service = serviceInstance.getServiceInfo(serviceName);

            if (!serviceInstance.isOk() || !service.isOk()) {
                continue;
            }

            if (instance == serviceInstance) {
                hostnames.add("host." + service.getName() + ".service." + fullDomain);
            }
        }

        for (ServiceInstance.Service service : instance.getServiceInfo()) {
            hostnames.add("all." + service.getName() + ".service." + fullDomain);

            if (!instance.isOk() || !service.isOk()) {
                continue;
            }

            hostnames.add("global." + service.getName() + ".service." + fullDomain);

            if (instance.isInLocation(location)) {
                hostnames.add("zone." + service.getName() + ".service." + fullDomain);
            }

            if (instance.isNearLocation(location)) {
                hostnames.add("region." + service.getName() + ".service." + fullDomain);
            }
        }

        for (String primaryKey : primaryMap.keySet()) {
            final String serviceName = primaryKey.split(":")[0];
            final String primaryName = primaryKey.split(":")[1];
            Object primaryValue = primaryMap.get(primaryKey);

            if (primaryValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<ServiceInstance> primaryInstances = (List<ServiceInstance>) primaryValue;
                ServiceInstance primaryInstance = null;

                for (ServiceInstance i : primaryInstances) {
                    ServiceInstance.Service service = i.getServiceInfo(serviceName);

                    if (service.isOk()) {
                        primaryInstance = i;
                        break;
                    }
                }

                if (instance.equals(primaryInstance)) {
                    hostnames.add(primaryName + "." + serviceName + ".service." + fullDomain);
                }

            } else if (primaryValue instanceof String) {
                String primaryInstanceId = (String) primaryValue;

                if (primaryInstanceId.equals(instance.getId())) {
                    hostnames.add(primaryName + "." + serviceName + ".service." + fullDomain);
                }
            }
        }

        return hostnames;
    }

    private void replicateServices(ServiceInstance serviceInstance, String primariesByService, String serviceChecks) throws Exception {
        if (REPLICATIONS.isEmpty()) {
            nextReplication = System.currentTimeMillis() + BATCH_INTERVAL;
        }

        ReplicationData replicationData = new ReplicationData(serviceInstance, primariesByService, serviceChecks);
        REPLICATIONS.put(serviceInstance.getId(), replicationData);

        String localHost = RootConfig.getLocalHostIp(ServerCommand.interfaceName);
        boolean mask = false;
        mask = ServiceState.getReplicationMask(localHost);

        if (mask) {
            LOGGER.debug("Hold replication from " + localHost);
        }

        if (nextReplication < System.currentTimeMillis() && !mask) {
            LOGGER.debug("Sending replication from " + localHost);

            List<ReplicationData> replications = new ArrayList<>();
            replications.addAll(REPLICATIONS.values());
            REPLICATIONS.clear();

            Queue<String> allHosts = new LinkedList<>();
            allHosts.addAll(RootConfig.getBeamServer(serverDNS, ServerCommand.interfaceName));

            String nextServer = allHosts.poll();

            while (nextServer != null) {

                if (nextServer.equals(localHost)) {
                    nextServer = allHosts.poll();
                    continue;
                }

                try {
                    LOGGER.debug("Forwarding service checks to {}", nextServer);

                    ServiceState.addPropagation(localHost);
                    LOGGER.debug("Replication counter: {}", ServiceState.getPropagationCounter(localHost));

                    Replicate replicate = new Replicate(nextServer, replications);
                    replicationExecutor.execute(replicate);

                } catch (Exception error) {
                    LOGGER.error("Fail to replicate to {}", nextServer);
                }

                nextServer = allHosts.poll();
            }
        }
    }

    private static class Replicate implements Runnable {

        private String url;
        private List<ReplicationData> replicationList;

        public Replicate(String server, List<ReplicationData> replicationList) {
            this.url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + server + ":" + ServerCommand.port + "/v2/replication");

            this.replicationList = replicationList;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("replication");

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                RequestConfig config = RequestConfig.custom().
                        setSocketTimeout(ServerCommand.timeout).
                        setConnectTimeout(ServerCommand.timeout).
                        setConnectionRequestTimeout(ServerCommand.timeout).
                        build();

                RequestBuilder requestBuilder = RequestBuilder.post().
                        setUri(url).
                        setConfig(config).
                        addParameter("senderIp", RootConfig.getLocalHostIp(ServerCommand.interfaceName)).
                        addParameter("timeStamp", Long.toString(System.currentTimeMillis()));

                for (ReplicationData replication : replicationList) {

                    ServiceInstance serviceInstance = replication.getServiceInstance();
                    String primariesByService = replication.getPrimariesByService();
                    String serviceChecks = replication.getServiceChecks();

                    requestBuilder.addParameter("instanceId", serviceInstance.getId());
                    requestBuilder.addParameter("layer", serviceInstance.getLayer());
                    requestBuilder.addParameter("environment", serviceInstance.getEnvironment());
                    requestBuilder.addParameter("launched", serviceInstance.getLaunchTime().toString());
                    requestBuilder.addParameter("location", serviceInstance.getLocation());
                    requestBuilder.addParameter("ip", serviceInstance.getPrivateIpAddress());
                    requestBuilder.addParameter("primariesByService", primariesByService);
                    requestBuilder.addParameter("serviceChecks", serviceChecks);
                }

                HttpUriRequest request = requestBuilder.build();

                try (CloseableHttpResponse response = client.execute(request)) {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        LOGGER.warn("Replication returned none 200 status code: " + response.getStatusLine());
                    }
                }
            } catch (IOException ioe) {
                LOGGER.warn("Replication to " + url + " failed", ioe);
            }

            String localHost = RootConfig.getLocalHostIp(ServerCommand.interfaceName);
            ServiceState.removePropagation(localHost);
            LOGGER.debug("Replication counter: {}", ServiceState.getPropagationCounter(localHost));
        }
    }

    public static class PrimaryCacheKey {
        public String env;
        public String region;
        public String key;

        public PrimaryCacheKey(String env, String region, String key) {
            this.env = env;
            this.region = region;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PrimaryCacheKey that = (PrimaryCacheKey) o;

            if (env != null ? !env.equals(that.env) : that.env != null) return false;
            if (region != null ? !region.equals(that.region) : that.region != null) return false;
            return key.equals(that.key);

        }

        @Override
        public int hashCode() {
            int result = env != null ? env.hashCode() : 0;
            result = 31 * result + (region != null ? region.hashCode() : 0);
            result = 31 * result + key.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "PrimaryCacheKey{" +
                    "env='" + env + '\'' +
                    ", region='" + region + '\'' +
                    ", key='" + key + '\'' +
                    '}';
        }
    }

    public static class ReplicationData {
        private ServiceInstance serviceInstance;
        private String primariesByService;
        private String serviceChecks;

        public ReplicationData(ServiceInstance serviceInstance, String primariesByService, String serviceChecks) {
            this.serviceInstance = serviceInstance;
            this.primariesByService = primariesByService;
            this.serviceChecks = serviceChecks;
        }

        public ServiceInstance getServiceInstance() {
            return serviceInstance;
        }

        public void setServiceInstance(ServiceInstance serviceInstance) {
            this.serviceInstance = serviceInstance;
        }

        public String getPrimariesByService() {
            return primariesByService;
        }

        public void setPrimariesByService(String primariesByService) {
            this.primariesByService = primariesByService;
        }

        public String getServiceChecks() {
            return serviceChecks;
        }

        public void setServiceChecks(String serviceChecks) {
            this.serviceChecks = serviceChecks;
        }
    }
}
