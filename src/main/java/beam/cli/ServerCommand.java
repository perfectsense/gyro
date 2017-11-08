package beam.cli;

import beam.BeamCloud;
import beam.BeamRuntime;
import beam.aws.GatewayCredentialsApiHandler;
import beam.config.RootConfig;
import beam.handlers.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;
import io.undertow.Undertow;
import io.undertow.predicate.Predicates;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;
import us.monoid.web.TextResource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Command(name = "start", description = "Start beam in server mode. Defaults to port 8601.")
public class ServerCommand implements Runnable {

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);

    @Option(required = true, name = {"--internalDomain"}, description = "Internal domain for project.")
    public String internalDomain;

    @Option(required = true, name = {"--name"}, description = "Name of project.")
    public String name;

    @Option(required = true, name = {"--serial"}, description = "Serial of project.")
    public String serial;

    @Option(required = true, name = {"--env"}, description = "Environment of project.")
    public String environment;

    @Option(name = {"--region"}, description = "Region primary data hosted in.")
    public String region;

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public Boolean debug;

    @Option(name = {"--dns"}, description = "DNS of beam servers.")
    public static String serverDNS;

    @Option(name = {"--interval"}, description = "Interval of replication")
    public Integer batchInterval;

    @Option(name = {"--port"}, description = "Port of handlers")
    public static String port;

    @Option(name = {"--propagate"}, description = "Propagate replications")
    public static boolean propagate;

    @Option(name = {"--interface"}, description = "Network interface of localhost address")
    public static String interfaceName;

    @Option(name = {"--config"}, description = "Location of the config file")
    public String configPath;

    @Option(name = {"--convergeInterval"}, description = "Interval of converge primaries and marks")
    public Integer convergeInterval;

    @Option(name = {"--converge"}, description = "Periodically updating the primaries and marks.")
    public Boolean converge;

    public static Integer timeout;
    public static Integer serviceCooldown;

    @Override
    public void run() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        AbstractCloudCommand.getCommandConfig("server", ServerCommand.class, this, configPath);

        if (debug == null) {
            debug = false;
        }

        if (batchInterval == null) {
            batchInterval = 0;
        }

        if (ObjectUtils.isBlank(port)) {
            port = "8601";
        }

        if (timeout == null) {
            timeout = 10000;
        }

        if (serviceCooldown == null) {
            serviceCooldown = 60000;
        }

        if (debug) {
            root.setLevel(Level.DEBUG);
        }

        BeamRuntime runtime = null;
        try {
            runtime = BeamRuntime.getCurrentRuntime();
        } catch (Exception ex) {
        }

        if (runtime == null) {
            runtime = new BeamRuntime(environment, name, null, serial, internalDomain, new HashSet<BeamCloud>());
            BeamRuntime.setCurrentRuntime(runtime);
        }

        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(RootConfig.getBeamServer(serverDNS, ServerCommand.interfaceName));

        String nextServer = allHosts.poll();
        String localHost = RootConfig.getLocalHostIp(interfaceName);

        TextResource response = null;

        while (nextServer != null && (response == null || !response.status(200))) {

            if (nextServer.equals(localHost)) {
                nextServer = allHosts.poll();
                continue;
            }

            try {
                LOGGER.info(String.format("Getting service discovery state from %s...", nextServer));

                response = new Resty().
                        setOptions(Resty.Option.timeout(timeout)).
                        text(com.psddev.dari.util.StringUtils.addQueryParameters(
                                "http://" + nextServer + ":" + port + "/v2/getState",
                                "environment", BeamRuntime.getCurrentRuntime().getEnvironment(),
                                "getState", true));


            } catch (IOException error) {
                nextServer = allHosts.poll();
                response = null;
            }
        }

        if (response != null && !response.status(500)) {

            try {
                Map<String, Object> json = (Map<String, Object>) ObjectUtils.fromJson(response.toString());

                String state = (String) json.get("state");
                Map<String, List<String>> markMap = (Map<String, List<String>>) json.get("mark");
                Map<String, List<String>> primaryMap = (Map<String, List<String>>) json.get("primary");

                try {
                    File stateFile = new File(ServiceState.INSTANCE_STATE_FILE);

                    if (!stateFile.exists()) {
                        stateFile.createNewFile();
                    }

                    PrintWriter out = new PrintWriter(stateFile);
                    out.write(state);
                    out.flush();

                } catch (Exception error) {
                    LOGGER.error(String.format("Fail to write to state file."));
                }

                for (String instanceId : markMap.keySet()) {

                    ServiceInstance.Mark mark;
                    List<String> markList = markMap.get(instanceId);

                    String name = markList.get(0);
                    String timeString = markList.get(1);

                    if ("Unavailable".equals(name)) {
                        mark = ServiceInstance.Mark.UNAVAILABLE;
                    } else {
                        mark = ServiceInstance.Mark.AVAILABLE;
                    }

                    mark.setTimeStamp(Long.parseLong(timeString));
                    ServiceState.markInstance(instanceId, mark);
                }

                for (String key : primaryMap.keySet()) {

                    ServiceState.PrimaryData primary;
                    List<String> primaryList = primaryMap.get(key);

                    String instanceId = primaryList.get(0);
                    String timeString = primaryList.get(1);

                    primary = new ServiceState.PrimaryData(instanceId, Long.parseLong(timeString));
                    ServiceState.setPrimaryData(key, primary);
                }

            } catch (Exception error) {
                LOGGER.error(String.format("Fail to get service discovery state!"));
            }
        }

        RootConfig rootConfig = runtime.getConfig();

        PathHandler pathHandler = new PathHandler();
        pathHandler.addPrefixPath("/v1/aws-credentials", new GatewayCredentialsApiHandler());
        pathHandler.addPrefixPath("/v1/tkt-auth", new TicketAuthApiHandler(rootConfig));

        pathHandler.addPrefixPath("/v2/hostsfile", new HostsfileApiHandler2(debug, region, batchInterval, serverDNS));
        pathHandler.addPrefixPath("/v2/instance", new InstanceHandler());
        pathHandler.addPrefixPath("/v2/markInstance", new MarkInstanceHandler());
        pathHandler.addPrefixPath("/v2/getPrimary", new GetPrimaryHandler());
        pathHandler.addPrefixPath("/v2/setPrimary", new SetPrimaryHandler());
        pathHandler.addPrefixPath("/v2/getState", new GetStateHandler(System.currentTimeMillis() + serviceCooldown));
        pathHandler.addPrefixPath("/v2/replication", new ReplicationHandler());
        pathHandler.addPrefixPath("/v2/getMonitor", new GetMonitorHandler());

        ResourceHandler resourceHandler = new ResourceHandler(new ClassPathResourceManager(ServerCommand.class.getClassLoader(), ""));
        PredicateHandler predicateHandler = new PredicateHandler(Predicates.suffixes(".css", ".js"), resourceHandler, pathHandler);

        Undertow server = Undertow.builder()
                .addHttpListener(Integer.parseInt(port), "0.0.0.0")
                .setHandler(predicateHandler).build();
        server.start();

        try {
            executeCommand();
        } catch (Exception ex) {
            LOGGER.error("Fails to replicate primary and mark");
        }
    }

    public void executeCommand() throws Exception {
        if (converge == null) {
            converge = true;
        }

        if (convergeInterval == null) {
            convergeInterval = 30 * 60 * 1000;
        }

        while (converge) {

            Thread.sleep(convergeInterval);

            try {
                convergePrimaryMark();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void convergePrimaryMark() {
        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(RootConfig.getBeamServer(serverDNS, ServerCommand.interfaceName));
        String nextServer = allHosts.poll();
        String localHost = RootConfig.getLocalHostIp(interfaceName);
        TextResource response = null;

        while (nextServer != null) {
            if (nextServer.equals(localHost)) {
                nextServer = allHosts.poll();
                continue;
            }

            try {
                LOGGER.debug(String.format("Converging service discovery primaries and marks from %s...", nextServer));

                response = new Resty().
                        setOptions(Resty.Option.timeout(timeout)).
                        text(com.psddev.dari.util.StringUtils.addQueryParameters(
                                "http://" + nextServer + ":" + port + "/v2/getState",
                                "environment", BeamRuntime.getCurrentRuntime().getEnvironment(),
                                "instanceState", false,
                                "getState", true));

                if (response != null && response.status(200)) {

                    try {
                        Map<String, Object> json = (Map<String, Object>) ObjectUtils.fromJson(response.toString());
                        Map<String, List<String>> markMap = (Map<String, List<String>>) json.get("mark");
                        Map<String, List<String>> primaryMap = (Map<String, List<String>>) json.get("primary");

                        for (String instanceId : markMap.keySet()) {

                            ServiceInstance.Mark mark;
                            List<String> markList = markMap.get(instanceId);

                            String name = markList.get(0);
                            String timeString = markList.get(1);

                            if ("Unavailable".equals(name)) {
                                mark = ServiceInstance.Mark.UNAVAILABLE;
                            } else {
                                mark = ServiceInstance.Mark.AVAILABLE;
                            }

                            long oldTimeStamp = ServiceState.getMark(instanceId) == null ? -1 : ServiceState.getMark(instanceId).getTimeStamp();
                            long newTimeStamp = Long.parseLong(timeString);
                            if (oldTimeStamp < newTimeStamp) {
                                mark.setTimeStamp(newTimeStamp);
                                ServiceState.markInstance(instanceId, mark);
                            }
                        }

                        for (String key : primaryMap.keySet()) {

                            ServiceState.PrimaryData primary;
                            List<String> primaryList = primaryMap.get(key);

                            String instanceId = primaryList.get(0);
                            String timeString = primaryList.get(1);

                            long oldTimeStamp = ServiceState.getPrimaryData(key) == null ? -1 : ServiceState.getPrimaryData(key).getTimeStamp();
                            long newTimeStamp = Long.parseLong(timeString);

                            if (oldTimeStamp < newTimeStamp) {
                                primary = new ServiceState.PrimaryData(instanceId, newTimeStamp);
                                ServiceState.setPrimaryData(key, primary);
                            }
                        }

                    } catch (Exception error) {
                        LOGGER.error(String.format("Fail to converge primaries and marks with %s", nextServer));
                    }
                }

            } catch (IOException error) {
                LOGGER.error(error.getMessage());
            } finally {
                nextServer = allHosts.poll();
            }
        }
    }
 }
