package beam.cli;

import beam.config.CloudConfig;
import beam.config.NetworkConfig;
import beam.config.RootConfig;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

import beam.BeamCloud;
import beam.BeamException;
import beam.BeamInstance;
import beam.BeamRuntime;
import beam.BeamStorage;

import com.psddev.dari.util.ObjectUtils;
import us.monoid.web.Resty;
import us.monoid.web.TextResource;

@Command(name = "primary", description = "Designate an instance as the primary for a service.")
public class PrimaryCommand extends AbstractCloudCommand implements AuditableCommand {

    private static final Table TABLE = new Table().
            addColumn("#", 4).
            addColumn("Instance ID", 20).
            addColumn("Environment", 12).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("Hostname", 65);

    @Option(required = true, name = { "-s", "--service" }, description = "Name of the service.")
    public String serviceName;

    @Option(required = true, name = { "-p", "--primary" }, description = "Name of the primary.")
    public String primaryName;

    @Option(name = { "-i", "--instance-id" }, description = "Instance ID to set as the primary.")
    public String newInstanceId;

    @Option(name = { "-a", "--automatic" }, description = "")
    public boolean automatic;

    @Option(name = { "-f", "--force" }, description = "Force the change without requiring user confirmation.")
    public boolean force;

    @Option(name = { "-r", "--region" }, description = "Set the primary for a region.")
    public String region;

    @Option(name = { "--list" }, description = "Show list of current primaries.")
    public boolean listOnly;

    @Option(name = {"--port"}, description = "Port of beam servers.")
    public String serverPort;

    @Option(name = { "--refresh" }, description = "Refresh the instance data from the cloud provider.")
    public boolean refresh;

    @Option(name = { "--nocheck" }, description = "Suppress instance service and primary validation.")
    public boolean nocheck;

    public static final String PRIMARY_API_PATH = "/v2/setPrimary";

    public static final String SERVICE_API_PATH = "/v2/getPrimary";

    public static final String INSTANCE_API_PATH = "/v2/instance";

    private static boolean selected = false;

    private String targetServer;

    public static String getStorageKey(BeamRuntime runtime, String serviceName, String primaryName) {
        return getStorageKey(runtime, serviceName, primaryName, null);
    }

    public static String getStorageKey(BeamRuntime runtime, String serviceName, String primaryName, String regionName) {
        if (regionName != null) {
            return "serial-" + runtime.getSerial() + "/service-" + serviceName + ".primary-" + primaryName + "-" + regionName.toLowerCase();
        }

        return "serial-" + runtime.getSerial() + "/service-" + serviceName + ".primary-" + primaryName;
    }

    @Override
    protected CloudHandler getCloudHandler() {
        return new CloudHandler() {

            @Override
            public void each(BeamCloud cloud) throws Exception {

                if (ObjectUtils.isBlank(serverPort)) {
                    serverPort = "8601";
                }

                BeamRuntime runtime = BeamRuntime.getCurrentRuntime();
                List<? extends BeamInstance> instances = cloud.getInstances(!refresh);

                RootConfig rootConfig = runtime.getConfig();
                NetworkConfig networkConfig = rootConfig.getNetworkConfig();

                for (CloudConfig cloudConfig : networkConfig.getClouds()) {
                    for (String region : cloudConfig.getActiveRegions()) {
                        if (!selected) {
                            displayRegion(region.toUpperCase(), instances);
                        }
                    }
                }
            }
        };
    }

    private void displayRegion(String region, List<? extends BeamInstance> instances) throws Exception {

        out.println("@|yellow Region: " + region + "|@");

        List<String> beamServerIps = new ArrayList<>();
        beamServerIps = findBeamServerIps(instances);

        BeamStorage storage = runtime.getStorage();
        String key = getStorageKey(runtime, serviceName, primaryName, region);

        String regionKey = getStorageKey(runtime, serviceName, primaryName, region);
        String defaultKey = getStorageKey(runtime, serviceName, primaryName, null);

        String oldInstanceId = fetchServiceStatus(regionKey, runtime.getEnvironment(), beamServerIps);
        Set<String> serviceInstanceIds = fetchServiceInstanceIds(serviceName, primaryName, beamServerIps);

        // Fallback to s3
        if (oldInstanceId == null) {
            oldInstanceId = storage.getString(key, region);
            if (oldInstanceId == null && "US-EAST-1".equals(region.toUpperCase())) {
                key = getStorageKey(runtime, serviceName, primaryName, null);
                oldInstanceId = storage.getString(key, region);
            }
        }

        List<BeamInstance> regionInstances = new ArrayList<>();
        for (BeamInstance instance : instances) {
            if (instance.getRegion().equalsIgnoreCase(region) &&
                    runtime.getEnvironment().equals(instance.getEnvironment()) &&
                    (nocheck || serviceInstanceIds.contains(instance.getId()))) {
                regionInstances.add(instance);
            }
        }

        if (regionInstances.size() == 0) {
            out.println("@|red No instances in this region.|@");
            return;
        }

        // Instance ID not specified in CLI so ask the user.
        if (!automatic && newInstanceId == null) {
            TABLE.writeHeader(out);

            int index = 0;

            // Only show instances that match the service.
            for (Iterator<? extends BeamInstance> i = regionInstances.iterator(); i.hasNext(); ) {
                BeamInstance instance = i.next();

                if (!runtime.getEnvironment().equals(instance.getEnvironment())) {
                    i.remove();

                } else {
                    ++index;

                    TABLE.writeRow(
                            out,
                            (instance.getId().equals(oldInstanceId) ? "* " : "  ") + index,
                            instance.getId(),
                            instance.getEnvironment(),
                            instance.getLocation(),
                            instance.getLayer(),
                            instance.getHostname());
                }
            }

            TABLE.writeFooter(out);

            if (listOnly) {
                return;
            }

            // Ask repeatedly until a valid choice is made.
            while (true && regionInstances.size() > 0) {
                if (ObjectUtils.isBlank(oldInstanceId)) {
                    out.format(
                            "Primary @|yellow %s|@ for the @|yellow %s|@ service is automatically chosen.",
                            primaryName,
                            serviceName);

                } else {
                    out.format(
                            "Instance @|yellow %s|@ is the primary @|yellow %s|@ for the @|yellow %s|@ service.",
                            oldInstanceId,
                            primaryName,
                            serviceName);
                }

                out.format(
                        " Change it? (#/a/N) ",
                        primaryName,
                        serviceName);
                out.flush();

                BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
                String pick = pickReader.readLine();
                Integer pickInt = ObjectUtils.to(Integer.class, pick);

                if ("a".equals(pick)) {
                    callPrimaryService("", regionKey, runtime.getEnvironment(), out, storage, region, beamServerIps);
                    callPrimaryService("", defaultKey, runtime.getEnvironment(), out, storage, region, beamServerIps);

                    writeChangedMessage(oldInstanceId, null, region);

                } else if (pickInt != null) {
                    if (pickInt > instances.size() || pickInt <= 0) {
                        out.format("@|red Must pick a number between 1 and %d!|@\n", instances.size());
                        continue;
                    }

                    newInstanceId = regionInstances.get(pickInt - 1).getId();

                    callPrimaryService(newInstanceId, regionKey, runtime.getEnvironment(), out, storage, region, beamServerIps);
                    callPrimaryService(newInstanceId, defaultKey, runtime.getEnvironment(), out, storage, region, beamServerIps);

                    writeChangedMessage(oldInstanceId, newInstanceId, region);

                    selected = true;

                } else {
                    out.format("@|red Not changed.|@\n");
                    out.flush();
                }

                break;
            }

        } else {

            // Valid options?
            if (automatic) {
                if (newInstanceId != null) {
                    throw new BeamException(
                            "Can't specify both -instance-id and -automatic options at the same time!");
                }

            } else {
                boolean newInstanceIdValid = false;

                for (BeamInstance instance : instances) {
                    if (instance.getId().equals(newInstanceId)) {
                        newInstanceIdValid = true;
                        break;
                    }
                }

                if (!newInstanceIdValid) {
                    throw new BeamException(String.format(
                            "[%s] isn't a valid instance ID!",
                            newInstanceId));
                }
            }

            if (ObjectUtils.equals(oldInstanceId, newInstanceId)) {
                throw new BeamException(String.format(
                        "[%s] primary for the [%s] service is already set to [%s]!",
                        primaryName,
                        serviceName,
                        ObjectUtils.firstNonNull(newInstanceId, "automatic")));
            }

            // Change the primary without user confirmation.
            if (force) {
                callPrimaryService(newInstanceId, regionKey, runtime.getEnvironment(), out, storage, null, beamServerIps);
                callPrimaryService(newInstanceId, defaultKey, runtime.getEnvironment(), out, storage, null, beamServerIps);

                writeChangedMessage(oldInstanceId, newInstanceId, region);
                out.flush();

                selected = true;

                // Ask for confirmation to change the primary.
            } else {
                String regionMessage = ObjectUtils.isBlank(region) ? "" :
                        String.format(" in %s region", region);
                out.format(
                        "Change the primary @|yellow %s|@ for the @|yellow %s|@ service from @|yellow %s|@ to @|yellow %s|@%s? (y/N) ",
                        primaryName,
                        serviceName,
                        ObjectUtils.firstNonNull(oldInstanceId, "automatic"),
                        ObjectUtils.firstNonNull(newInstanceId, "automatic"),
                        regionMessage);
                out.flush();

                BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));
                String confirm = ObjectUtils.to(String.class, confirmReader.readLine());

                if ("y".equalsIgnoreCase(confirm)) {
                    callPrimaryService(newInstanceId, regionKey, runtime.getEnvironment(), out, storage, null, beamServerIps);
                    callPrimaryService(newInstanceId, defaultKey, runtime.getEnvironment(), out, storage, null, beamServerIps);

                    out.format("@|green Changed.|@\n");
                    out.flush();

                    selected = true;

                } else {
                    out.format("@|red Not changed.|@\n");
                    out.flush();
                }
            }
        }

        out.println();
    }

    private void writeChangedMessage(String oldInstanceId, String newInstanceId, String region) {
        String regionMessage = ObjectUtils.isBlank(region) ? "" :
                String.format(" in %s region", region);

        out.format(
                "Changed the primary @|yellow %s|@ for the @|yellow %s|@ service from @|yellow %s|@ to @|yellow %s|@%s.\n",
                primaryName,
                serviceName,
                ObjectUtils.firstNonNull(oldInstanceId, "automatic"),
                ObjectUtils.firstNonNull(newInstanceId, "automatic"),
                regionMessage);
        out.flush();
    }

    private Set<String> fetchServiceInstanceIds(String service, String primary, List<String> beamServerIps) throws Exception {
        Queue<String> allHosts = new LinkedList<>();
        if (targetServer != null) {
            allHosts.add(targetServer);

        } else {
            allHosts.addAll(beamServerIps);
        }

        boolean success = false;
        Set<String> serviceInstanceIds = new HashSet<>();

        while (!allHosts.isEmpty() && !success) {
            String beamServer = allHosts.poll();
            String url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + beamServer + ":" + serverPort + INSTANCE_API_PATH);

            try {
                TextResource response = new Resty().
                        setOptions(Resty.Option.timeout(10000)).
                        text(url);

                Map<String, Object> json = (Map<String, Object>) ObjectUtils.fromJson(response.toString());
                if (json.get("instances") != null) {
                    List<Object> instances = (List) json.get("instances");

                    for (Object i : instances) {
                        Map<String, Object> instance = (Map<String, Object>) i;

                        if (instance.get("servicesInfo") != null) {
                            List<Map<String, Object>> servicesInfo = (List<Map<String, Object>>) (instance.get("servicesInfo"));
                            String instanceId = (String) instance.get("id");
                            for (Map<String, Object> eachService : servicesInfo) {
                                String name = (String) eachService.get("name");
                                List<String> primaries = (List<String>) eachService.get("primaries");
                                if (service.equals(name) && primaries.contains(primary)) {
                                    serviceInstanceIds.add(instanceId);
                                }
                            }
                        }
                    }
                }

                success = true;

            } catch (Exception error) {

            }
        }

        if (!success) {
            throw new BeamException(String.format("Unable to fetch service instance that has service @|yellow %s|@ and primary @|yellow %s|@.", service, primary));
        }

        return serviceInstanceIds;
    }

    private String fetchServiceStatus(String key, String environment, List<String> beamServerIps) throws Exception {
        String oldInstanceId = null;

        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(beamServerIps);
        String beamServer = null;

        while (!allHosts.isEmpty() && oldInstanceId == null) {
            beamServer = allHosts.poll();
            String url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + beamServer + ":" + serverPort + SERVICE_API_PATH,
                    "key", key, "environment", environment);

            boolean hasError = false;
            String errorMessage = "";

            try {
                TextResource response = new Resty().
                        setOptions(Resty.Option.timeout(10000)).
                        text(url);

                Map<String, Object> json = (Map<String, Object>) ObjectUtils.fromJson(response.toString());
                if (json.containsKey("error")) {
                    errorMessage = json.get("error").toString();
                    hasError = true;
                }

                oldInstanceId = (String)json.get("instanceId");

            } catch (Exception error) {
                oldInstanceId = null;
            }

            if (hasError) {
                throw new BeamException(errorMessage);
            }
        }

        targetServer = beamServer;
        if ("null".equals(oldInstanceId)) {
            oldInstanceId = null;
        }

        return oldInstanceId;
    }

    private void callPrimaryService(String newInstanceId, String key, String environment, PrintWriter out, BeamStorage storage, String region, List<String> beamServerIps) throws Exception {
        System.out.println(String.format("Setting instance %s as the primary for service %s...", newInstanceId, serviceName));

        boolean success = false;

        Queue<String> allHosts = new LinkedList<>();
        if (targetServer != null) {
            allHosts.add(targetServer);

        } else {
            allHosts.addAll(beamServerIps);
        }

        while (!allHosts.isEmpty() && !success) {
            String beamServer = allHosts.poll();
            String url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + beamServer + ":" + serverPort + PRIMARY_API_PATH,
                    "newInstanceId", newInstanceId,
                    "key", key,
                    "environment", environment,
                    "timeStamp", System.currentTimeMillis());

            try {
                TextResource response = new Resty().
                        setOptions(Resty.Option.timeout(10000)).
                        text(url);

                if (response.status(500)) {


                } else if (response.status(204)) {
                    System.out.println("The beam primary request has an older timeStamp.");

                } else {
                    setEverConfirmed(500);
                    success = true;
                    out.println("@|green Ok|@");
                }

            } catch (Exception error) {

            }
        }

        if (!success) {
            if (storage.doesExist(region)) {
                if ("".equals(newInstanceId)) {
                    storage.putString(region, key, null);

                } else if (region != null){
                    storage.putString(region, key, newInstanceId);

                } else {
                    storage.putString(key, newInstanceId);
                }

                setEverConfirmed(500);
                out.println("Write to s3 @|green Ok|@");

            } else {
                out.println("@|red Failed|@");
            }
        }
    }

    private List<String> findBeamServerIps(List<? extends BeamInstance> instances) throws Exception {
        List<String> beamServerIps = new ArrayList<>();

        for (BeamInstance instance : instances) {
            if (instance.getEnvironment().equals(runtime.getEnvironment())) {
                beamServerIps.add(instance.getPublicIpAddress());
                beamServerIps.add(instance.getPrivateIpAddress());
            }
        }

        for (BeamInstance instance : instances) {
            if (instance.getLayer().equals("gateway") && "prod".equals(runtime.getEnvironment())) {
                beamServerIps.add(instance.getPublicIpAddress());
                beamServerIps.add(instance.getPrivateIpAddress());
            }
        }

        return beamServerIps;
    }
}
