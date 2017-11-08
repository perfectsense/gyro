package beam.handlers;

import beam.BeamRuntime;
import com.psddev.dari.util.ObjectUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class GetStateHandler implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GetStateHandler.class);

    private static long readyTime;

    public GetStateHandler(long readyTime) {
        this.readyTime = readyTime;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Deque<String>> params = exchange.getQueryParameters();

        boolean getState = params.get("getState") != null;
        boolean instanceState = params.get("instanceState") == null;

        String environment = params.get("environment") != null ? params.get("environment").element() : null;
        String env = BeamRuntime.getCurrentRuntime().getEnvironment();

        Map<String, Object> response = new HashMap<>();

        if (readyTime > System.currentTimeMillis()) {
            LOGGER.debug(String.format("Skip getState request from %s", exchange.getSourceAddress().toString()));
            exchange.setStatusCode(500);
            return;
        }

        if (getState && (env.equals(environment))) {
            try {
                if (instanceState) {
                    String content = new Scanner(new File(ServiceState.INSTANCE_STATE_FILE)).useDelimiter("\\Z").next();
                    response.put("state", content);
                }

                Map<String, List<String>> markMap = new HashMap<>();
                for (String instanceId : ServiceState.getInstanceMarks().keySet()) {

                    ServiceInstance.Mark mark = ServiceState.getMark(instanceId);

                    List<String> markList = new ArrayList<>();
                    markList.add(mark.getName());
                    markList.add(Long.toString(mark.getTimeStamp()));

                    markMap.put(instanceId, markList);
                }

                Map<String, List<String>> primaryMap = new HashMap<>();
                for (String key : ServiceState.getServicePrimaries().keySet()) {

                    ServiceState.PrimaryData primary = ServiceState.getPrimaryData(key);

                    List<String> primaryList = new ArrayList<>();
                    primaryList.add(primary.getInstanceId());
                    primaryList.add(Long.toString(primary.getTimeStamp()));

                    primaryMap.put(key, primaryList);
                }

                response.put("mark", markMap);
                response.put("primary", primaryMap);

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
                exchange.getResponseSender().send(ObjectUtils.toJson(response, true));
                exchange.endExchange();

            } catch (Exception ex) {
                LOGGER.error("GetState request failed.", ex);
                exchange.setStatusCode(500);
            }

            return;
        }
    }
}
