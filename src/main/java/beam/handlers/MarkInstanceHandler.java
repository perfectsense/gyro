package beam.handlers;

import beam.cli.ServerCommand;
import beam.config.RootConfig;
import com.psddev.dari.util.ObjectUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;

import java.util.*;

import static beam.handlers.ServiceInstance.Mark;

public class MarkInstanceHandler implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MarkInstanceHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        String instanceId = params.get("instanceId") != null ? params.get("instanceId").element() : null;
        String markType = params.get("mark") != null ? params.get("mark").element() : null;
        String timeString = params.get("timeStamp") != null ? params.get("timeStamp").element() : null;
        String serverDNS = ServerCommand.serverDNS;

        if (ObjectUtils.isBlank(timeString)) {
            LOGGER.error("Mark timeStamp is blank.");
            return;
        }

        if (ObjectUtils.isBlank(instanceId)) {
            LOGGER.error("Mark instanceId is blank.");
            return;
        }

        long timeStamp = Long.parseLong(timeString);
        long oldStamp = -1;

        Map<String, String> response = new HashMap<>();
        try {
            Mark mark = Mark.valueOf(markType);
            mark.setTimeStamp(timeStamp);

            if (mark != null) {

                if (ServiceState.getInstanceMarks().containsKey(instanceId)) {
                    oldStamp = ServiceState.getMark(instanceId).getTimeStamp();
                }

                if (timeStamp > oldStamp) {
                    LOGGER.info("Newer timeStamp, marking instance and replicating...");
                    ServiceState.markInstance(instanceId, mark);
                    replicateMarks(instanceId, mark, serverDNS);

                }  else {
                    LOGGER.info("Older timeStamp, throwing away...");
                    exchange.setStatusCode(204);
                }
            }

            response.put("status", "ok");
            response.put("mark", mark.toString());

        } catch (Exception ex) {
            LOGGER.error("Mark request failed.", ex);

            response.put("status", "failed");
            response.put("reason", ex.getMessage());

            exchange.setStatusCode(500);
            exchange.getResponseSender().send("UNKOWN_MARK");
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
        exchange.getResponseSender().send(ObjectUtils.toJson(response, true));
    }

    private void replicateMarks(String instanceId, Mark mark, String serverDNS) throws Exception {

        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(RootConfig.getBeamServer(serverDNS, ServerCommand.interfaceName));

        String nextServer = allHosts.poll();
        String localHost = RootConfig.getLocalHostIp(ServerCommand.interfaceName);

        while (nextServer != null) {

            if (nextServer.equals(localHost)) {
                nextServer = allHosts.poll();
                continue;
            }

            try {
                new Replicate(nextServer, instanceId, mark, serverDNS).start();

            } catch (Exception error) {
                LOGGER.error("Replicate mark data failed.", error);
            }

            nextServer = allHosts.poll();
        }
    }

    private static class Replicate extends Thread {

        private final String url;

        public Replicate(String server, String instanceId, Mark mark, String serverDNS) {
            this.url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + server + ":" + ServerCommand.port + "/v2/markInstance",
                    "instanceId", instanceId,
                    "mark", mark.toString(),
                    "serverDNS", serverDNS,
                    "timeStamp", mark.getTimeStamp());
        }

        @Override
        public void run() {
            try {
                new Resty().setOptions(Resty.Option.timeout(ServerCommand.timeout)).text(url);
            } catch (Exception error) {
            }
        }
    }

}