package beam.handlers;

import beam.BeamRuntime;
import beam.cli.ServerCommand;
import beam.config.RootConfig;
import beam.handlers.ServiceState.PrimaryData;

import com.psddev.dari.util.ObjectUtils;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import us.monoid.web.Resty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;

public class SetPrimaryHandler implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SetPrimaryHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        String value = params.get("newInstanceId") != null ? params.get("newInstanceId").element() : null;
        String key = params.get("key") != null ? params.get("key").element() : null;
        String environment = params.get("environment") != null ? params.get("environment").element() : null;
        String timeString = params.get("timeStamp") != null ? params.get("timeStamp").element() : null;
        String serverDNS = ServerCommand.serverDNS;

        String env = BeamRuntime.getCurrentRuntime().getEnvironment();
        boolean envMatch = false;

        if (ObjectUtils.isBlank(key)) {
            LOGGER.error("Primary key is blank.");
            return;

        } else if (ObjectUtils.isBlank(timeString)) {
            LOGGER.error("Primary timeStamp is blank.");
            return;
        }

        if (environment == null) {
            LOGGER.error("Primary environment is blank.");
            exchange.setStatusCode(204);
            return;
        } else if (environment.equals("prod")) {
            if (env.equals("prod") || env.equals("network")) {
                envMatch = true;
            }
        } else {
            envMatch = environment.equals(env);
        }

        if (envMatch) {
            long timeStamp = Long.parseLong(timeString);
            long oldStamp = -1;

            try {
                ServiceState.PrimaryData primaryData = ServiceState.getPrimaryData(key);
                oldStamp = primaryData.getTimeStamp();

            } catch (Exception error) {
            }

            if (timeStamp > oldStamp) {

                LOGGER.info(String.format("Newer timeStamp from %s, setting primary and replicating...", exchange.getSourceAddress().getAddress().toString()));

                try {
                    ServiceState.setPrimaryData(key, new ServiceState.PrimaryData(value, timeStamp));
                    replicatePrimary(key, value, serverDNS, timeStamp);

                } catch (Exception ex) {
                    LOGGER.error("Set primary request failed.", ex);

                    exchange.setStatusCode(500);
                }

            } else {
                LOGGER.info(String.format("Older timeStamp from %s, throwing away...", exchange.getSourceAddress().getAddress().toString()));
                exchange.setStatusCode(204);
            }
        } else {
            LOGGER.error(String.format("Environment mismatch, %s -> %s", environment, env));
        }
    }

    private void replicatePrimary(String key, String value, String serverDNS, long timeStamp) throws Exception {
        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(RootConfig.getBeamServer(serverDNS, ServerCommand.interfaceName));

        String nextServer = allHosts.poll();
        String localHost = RootConfig.getLocalHostIp(ServerCommand.interfaceName);

        while (nextServer != null) {

            if (nextServer.equals(localHost)) {
                nextServer = allHosts.poll();
                continue;
            }

            LOGGER.info("Replicate primary data to: " + nextServer);

            try {
                new Replicate(key, value, nextServer, timeStamp, serverDNS).start();

            } catch (Exception error) {
                LOGGER.error("Replicate primary data failed.", error);
            }

            nextServer = allHosts.poll();
        }
    }

    private static class Replicate extends Thread {

        private final String url;

        public Replicate(String key, String value, String server, long timeStamp, String serverDNS) {
            this.url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + server + ":" + ServerCommand.port + "/v2/setPrimary",
                    "newInstanceId", value,
                    "key", key,
                    "environment", BeamRuntime.getCurrentRuntime().getEnvironment(),
                    "timeStamp", timeStamp,
                    "serverDNS", serverDNS);
        }

        @Override
        public void run() {
            try {
                new Resty().setOptions(Resty.Option.timeout(ServerCommand.timeout)).text(url);
            } catch (Exception error) {
            }
        }
    }

    public static synchronized void load() {
        File stateFile = new File(ServiceState.PRIMARY_STATE_FILE);
        if (stateFile.exists()) {
            DumperOptions options = new DumperOptions();
            options.setWidth(80);
            options.setIndent(4);

            Yaml yaml = new Yaml(options);
            try {
                Map<String, PrimaryData> primaries = (Map<String, PrimaryData>) yaml.load(new FileInputStream(stateFile));
                ServiceState.putAllPrimaries(primaries);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static synchronized void save() throws Exception {
        File stateFile = new File(ServiceState.PRIMARY_STATE_FILE);
        DumperOptions options = new DumperOptions();
        options.setWidth(80);
        options.setIndent(4);

        Yaml yaml = new Yaml(options);
        FileWriter filewriter = new FileWriter(stateFile);
        try {
            ServiceState.writePrimaries(yaml, filewriter);
        } finally {
            filewriter.close();
        }
    }
}