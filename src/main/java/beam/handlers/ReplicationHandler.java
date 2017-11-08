package beam.handlers;

import beam.cli.ServerCommand;
import beam.config.RootConfig;
import com.amazonaws.util.IOUtils;
import com.google.common.base.Preconditions;
import com.psddev.dari.util.ObjectUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.QueryParameterUtils;
import org.slf4j.LoggerFactory;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.*;
import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReplicationHandler implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReplicationHandler.class);
    private static final Executor replicationExecutor = Executors.newFixedThreadPool(100);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();

        String postData = IOUtils.toString(exchange.getInputStream());
        Map<String, Deque<String>> params = QueryParameterUtils.parseQueryString(postData, "UTF-8");

        Deque<String> instanceIds = params.get("instanceId") != null ? params.get("instanceId") : null;
        Deque<String> environments = params.get("environment") != null ? params.get("environment") : null;
        Deque<String> layers = params.get("layer") != null ? params.get("layer") : null;
        Deque<String> locations = params.get("location") != null ? params.get("location") : null;
        Deque<String> ips = params.get("ip") != null ? params.get("ip") : null;
        Deque<String> launchedStrings = params.get("launched") != null ? params.get("launched") : null;
        Deque<String> primariesByServiceStrings = params.get("primariesByService") != null ? params.get("primariesByService") : null;
        Deque<String> serviceChecksStrings = params.get("serviceChecks") != null ? params.get("serviceChecks") : null;

        String senderIp = params.get("senderIp") != null ? params.get("senderIp").element() : null;
        String timeString = params.get("timeStamp") != null ? params.get("timeStamp").element() : null;

        if (ObjectUtils.isBlank(senderIp)) {
            LOGGER.error("Replication senderIp is blank.");
            return;

        } else if (ObjectUtils.isBlank(timeString)) {
            LOGGER.error("Replication timeStamp is blank.");
            return;

        } else if (ObjectUtils.isBlank(instanceIds)) {
            LOGGER.error("Replication instanceIds are blank.");
            return;
        }

        if (senderIp.equals(RootConfig.getLocalHostIp(ServerCommand.interfaceName))) {
            LOGGER.debug("Replication from the current host itself, throwing away...");
            return;
        }

        long timeStamp = Long.parseLong(timeString);
        long oldStamp = -1;

        try {
            oldStamp = ServiceState.getReplicationStatus(senderIp);

        } catch (Exception error) {
        }

        if (timeStamp > oldStamp) {

            LOGGER.debug("Newer replication timeStamp, replicating...");

            try {
                ServiceState.setReplicationStatus(senderIp, timeStamp);

                List<HostsfileApiHandler2.ReplicationData> replicationList = new ArrayList<>();
                int size = instanceIds.size();

                for (int i = 0; i < size; i++) {
                    String instanceId = instanceIds.pollFirst();
                    String environment = environments.pollFirst();
                    String layer = layers.pollFirst();
                    String location = locations.pollFirst();
                    String ip = ips.pollFirst();
                    String launchedString = launchedStrings.pollFirst();
                    String primariesByServiceString = primariesByServiceStrings.pollFirst();
                    String serviceChecksString = serviceChecksStrings.pollFirst();

                    Preconditions.checkNotNull(environment, "environment");
                    Preconditions.checkNotNull(layer, "layer");
                    Preconditions.checkNotNull(location, "location");
                    Preconditions.checkNotNull(ip, "ip");
                    Preconditions.checkNotNull(launchedString, "launchedString");

                    ServiceInstance instance = new ServiceInstance();

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

                    HostsfileApiHandler2.ReplicationData data = new HostsfileApiHandler2.ReplicationData(instance, primariesByServiceString, serviceChecksString);
                    replicationList.add(data);
                }

                ServiceState.incrementReplication(exchange.getSourceAddress().getAddress().toString());

                if (!ServerCommand.propagate) {
                    LOGGER.debug("Propagate option is set to false.");

                } else {
                    LOGGER.debug("Propagate option is set to true.");

                    boolean mask = false;
                    mask = ServiceState.getReplicationMask(senderIp);

                    if (!mask) {
                        LOGGER.debug("Propagate replication from " + senderIp);
                        replicateServices(replicationList, senderIp, timeString);

                    } else {
                        LOGGER.debug("Hold propagating replication from " + senderIp);
                    }
                }

            } catch (Exception ex) {
                LOGGER.error("Replication request failed.", ex);

                exchange.setStatusCode(500);
            }

        } else {
            LOGGER.debug("Older replication timeStamp, throwing away...");
        }
    }

    private void replicateServices(List<HostsfileApiHandler2.ReplicationData> replications, String senderIp, String timeString) throws Exception {
        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(RootConfig.getBeamServer(ServerCommand.serverDNS, ServerCommand.interfaceName));

        String nextServer = allHosts.poll();
        String localHost = RootConfig.getLocalHostIp(ServerCommand.interfaceName);

        while (nextServer != null) {

            if (nextServer.equals(localHost)) {
                nextServer = allHosts.poll();
                continue;
            }

            try {
                LOGGER.debug("Propagate service checks to {}", nextServer);

                ServiceState.addPropagation(senderIp);
                LOGGER.debug("Propagate counter: {}", ServiceState.getPropagationCounter(senderIp));

                Replicate replicate = new Replicate(nextServer, replications, senderIp, timeString);
                replicationExecutor.execute(replicate);

            } catch (Exception error) {
                LOGGER.error("Fail to Propagate replication to {}", nextServer);
            }

            nextServer = allHosts.poll();
        }
    }

    private static class Replicate implements Runnable {

        private String url;
        private List<HostsfileApiHandler2.ReplicationData> replicationList;
        private String senderIp;
        private String timeString;

        public Replicate(String server, List<HostsfileApiHandler2.ReplicationData> replicationList, String senderIp, String timeString) {
            this.url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + server + ":" + ServerCommand.port + "/v2/replication");

            this.replicationList = replicationList;
            this.senderIp = senderIp;
            this.timeString = timeString;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("propagate");

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                RequestConfig config = RequestConfig.custom().
                        setSocketTimeout(ServerCommand.timeout).
                        setConnectTimeout(ServerCommand.timeout).
                        setConnectionRequestTimeout(ServerCommand.timeout).
                        build();

                RequestBuilder requestBuilder = RequestBuilder.post().
                        setUri(url).
                        setConfig(config).
                        addParameter("senderIp", senderIp).
                        addParameter("timeStamp", timeString);

                for (HostsfileApiHandler2.ReplicationData replication : replicationList) {

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
                        LOGGER.warn("Propagate replication returned none 200 status code: " + response.getStatusLine());
                    }
                }
            } catch (IOException ioe) {
                LOGGER.warn("Propagate replication to " + url + " failed", ioe);
            }

            ServiceState.removePropagation(senderIp);
            LOGGER.debug("Propagate counter: {}", ServiceState.getPropagationCounter(senderIp));
        }
    }
}

