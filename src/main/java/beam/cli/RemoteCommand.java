package beam.cli;

import beam.BeamException;
import beam.BeamRuntime;
import beam.aws.config.AWSCloudConfig;
import beam.aws.config.AWSRegionConfig;
import beam.aws.config.AWSZoneConfig;
import beam.config.CloudConfig;
import beam.config.GatewayConfig;
import beam.config.RootConfig;
import beam.config.SubnetConfig;
import beam.utils.RestyReadTimeout;
import com.google.common.net.InetAddresses;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import us.monoid.web.Resty;
import us.monoid.web.TextResource;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

public abstract class RemoteCommand implements Runnable {

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoteCommand.class);

    @Option(name = {"--server"}, description = "Hostname/IP of beam server.")
    public String serverInfo;

    @Option(name = {"--port"}, description = "Port of beam server.")
    public String serverPort;

    @Option(name = {"--timeout"}, description = "Timeout of call in ms.")
    public Integer timeout;

    @Option(name = {"--config"}, description = "Location of the config file")
    public String configPath;

    public static final String METADATA_URL = "http://169.254.169.254//latest/dynamic/instance-identity/document/";

    public abstract void executeCommand(PrintWriter out) throws Exception;

    @Override
    public void run() {
        AbstractCloudCommand.getCommandConfig("client", RemoteCommand.class, this, configPath);

        if (serverPort == null) {
            serverPort = "8601";
        }

        if (timeout == null) {
            timeout = 10000;
        }

        try {
            executeCommand(new PrintWriter(System.out));
        } catch (Exception ex) {

        }
    }

    @Command(name = "hostsfile", description = "Fetch hostsfile from beam server.")
    public static class HostsfileCommand extends RemoteCommand {

        @Arguments(title = "instanceId", description = "Generate hostsfile for specific host.")
        public String instanceId;

        @Option(name = {"--outfile"}, description = "Write hostsfile to file.")
        public String outfile;

        @Option(name = {"--reload"}, description = "Reload dnsmasq process.")
        public String pidfile;

        @Option(name = {"--layer"}, description = "Layer this host is in.")
        public String layer;

        @Option(name = {"--env"}, description = "Environment this host is in.")
        public String environment;

        @Option(name = {"--location"}, description = "Location this host is in.")
        public String location;

        @Option(name = {"--ip"}, description = "IP of client.")
        public String ip;

        @Option(name = {"--launched"}, description = "Time instance was launched.")
        public String launchTime;

        @Option(name = {"--daemon"}, description = "Stay running, periodically updating the hostsfile.")
        public Boolean daemon;

        @Option(name = {"--interval"}, description = "When -daemon is provided this sets the refresh interval in milliseconds.")
        public Integer interval;

        public static final String HOSTFILE_API_PATH = "/v2/hostsfile";

        private void updateHostsFile(PrintWriter out) throws Exception {

            Queue<String> allHosts = new LinkedList<>();
            String serverHost = null;
            String serverDNS = null;

            if (!ObjectUtils.isBlank(serverInfo)) {
                if (InetAddresses.isInetAddress(serverInfo)) {
                    serverHost = serverInfo;

                } else {
                    serverDNS = serverInfo;
                }
            }

            if (serverHost == null) {
                BeamRuntime runtime = null;
                try {
                    runtime = BeamRuntime.getCurrentRuntime();
                } catch (Exception ex) {
                    runtime = BeamRuntime.setCurrentRuntime("prod");
                }

                InetAddress gateway = findGateway(runtime);
                if (gateway != null) {
                    serverHost = gateway.getHostAddress();
                } else {
                    allHosts.addAll(RootConfig.getBeamServer(serverDNS, null));
                    serverHost = allHosts.poll();
                }

                if (serverHost == null) {
                    throw new RuntimeException("Unable to locate beam server automatically. Passing --server argument.");
                }
            }

            // Attempt to find instanceId from magic metadata URL.
            if (instanceId == null) {
                try {
                    instanceId = (String) new Resty().setOptions(Resty.Option.timeout(500)).
                            json(METADATA_URL).get("instanceId");
                } catch(IOException ioe) {
                    throw new RuntimeException("Unable to determine instanceId from metadata URL. Please pass it on the commandline.");
                }
            }

            // Check all services.
            File serviceDirectory = new File("/etc/beam/service.d");
            Map<String, Set<String>> primariesByService = new HashMap<String, Set<String>>();
            Map<String, Integer> serviceChecks = new HashMap<String, Integer>();

            if (serviceDirectory.isDirectory()) {
                ExecutorService executor = Executors.newCachedThreadPool();

                try {
                    Map<String, Future<Integer>> serviceCheckFutures = new HashMap<String, Future<Integer>>();

                    for (File file : serviceDirectory.listFiles()) {
                        if (file.isDirectory()) {
                            String name = file.getName();

                            File configFile = new File(file, "config.yml");

                            if (configFile.exists()) {
                                InputStream configInput = new FileInputStream(configFile);

                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> config = (Map<String, Object>) new Yaml().load(configInput);
                                    Set<String> primaries = primariesByService.get(name);
                                    @SuppressWarnings("unchecked")
                                    Collection<String> primariesFromConfig = (Collection<String>) config.get("primaries");

                                    if (primariesFromConfig != null) {
                                        if (primaries == null) {
                                            primaries = new HashSet<String>();
                                            primariesByService.put(name, primaries);
                                        }

                                        primaries.addAll(primariesFromConfig);
                                    }

                                } finally {
                                    configInput.close();
                                }
                            }

                            serviceCheckFutures.put(name, executor.submit(new ServiceCheck(new File(file, "check.sh"))));

                        } else if (file.isFile() && file.canExecute()) {
                            String name = file.getName();

                            if (name.endsWith(".sh")) {
                                name = name.substring(0, name.length() - 3);
                            }

                            serviceCheckFutures.put(name, executor.submit(new ServiceCheck(file)));
                        }
                    }

                    for (Map.Entry<String, Future<Integer>> entry : serviceCheckFutures.entrySet()) {
                        String name = entry.getKey();

                        try {
                            serviceChecks.put(name, entry.getValue().get());

                        } catch (Exception error) {
                            serviceChecks.put(name, -1);
                        }
                    }

                } finally {
                    executor.shutdown();
                }
            }

            TextResource response = null;

            do {
                try {
                    response = new Resty().
                            setOptions(Resty.Option.timeout(timeout), new RestyReadTimeout(timeout)).
                            text(StringUtils.addQueryParameters(
                                    "http://" + serverHost + ":" + serverPort + HOSTFILE_API_PATH,
                                    "instanceId", instanceId,
                                    "layer", layer,
                                    "environment", environment,
                                    "launched", launchTime,
                                    "location", location,
                                    "ip", ip,
                                    "primariesByService", ObjectUtils.toJson(primariesByService),
                                    "serviceChecks", ObjectUtils.toJson(serviceChecks)));

                    if (response == null) {
                        serverHost = allHosts.poll();
                    } else if (response.status(204) || response.status(500)) {
                        serverHost = allHosts.poll();
                        response = null;
                    }

                } catch (Exception error) {
                    serverHost = allHosts.poll();
                }

            } while (serverHost != null && response == null);

            if (response == null) {
                throw new BeamException("Unable to connect to any beam servers.");
            }

            String hostsfile = response.toString();

            if (hostsfile.length() < 10) {
                return;
            }

            if (outfile == null) {
                out.write(hostsfile);
                out.flush();
            } else {
                File file = new File(outfile);

                if (file.exists()) {
                    String newHash = DigestUtils.md5Hex(hostsfile);
                    String oldHash = DigestUtils.md5Hex(new FileInputStream(file));

                    if (oldHash.equals(newHash)) {
                        return;
                    }
                }

                OutputStreamWriter writer = new FileWriter(file);
                writer.write(hostsfile);
                writer.close();
            }

            if (pidfile != null) {
                File file = new File(pidfile);
                BufferedReader reader = new BufferedReader(new FileReader(file));

                try {
                    String pid = reader.readLine();

                    if (pid != null) {
                        LOGGER.debug("Hosts file changed.");
                        Runtime.getRuntime().exec("kill -HUP " + pid);
                    }

                } finally {
                    reader.close();
                }
            }
        }

        @Override
        public void executeCommand(PrintWriter out) throws Exception {

            AbstractCloudCommand.getCommandConfig("client", HostsfileCommand.class, this, configPath);

            if (daemon == null) {
                daemon = false;
            }

            if (interval == null) {
                interval = 5000;
            }

            do {
                try {
                    updateHostsFile(out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (daemon) {
                    Thread.sleep(interval);
                }
            } while (daemon);
        }
    }

    public InetAddress findGateway(BeamRuntime runtime) {
        RootConfig config = runtime.getConfig();

        if (config != null) {
            for (CloudConfig cloudConfig : config.getNetworkConfig().getClouds()) {
                if (cloudConfig instanceof AWSCloudConfig) {
                    for (AWSRegionConfig regionConfig : ((AWSCloudConfig) cloudConfig).getRegions()) {
                        for (AWSZoneConfig zoneConfig : regionConfig.getZones()) {
                            for (SubnetConfig subnet : zoneConfig.getSubnets()) {
                                GatewayConfig gatewayConfig = subnet.getGateway();

                                if (gatewayConfig == null) {
                                    continue;
                                }

                                try {
                                    InetAddress gatewayAddress = InetAddress.getByName(gatewayConfig.getIpAddress());

                                    if (gatewayAddress.isReachable(1000)) {
                                        return gatewayAddress;
                                    }

                                } catch (IOException error) {
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static class ServiceCheck implements Callable<Integer> {

        private final File file;

        public ServiceCheck(File file) {
            this.file = file;
        }

        @Override
        public Integer call() throws IOException, InterruptedException {
            return new ProcessBuilder().
                    command(file.toString()).
                    start().
                    waitFor();
        }
    }
}
