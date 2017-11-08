package beam.handlers;

import com.psddev.dari.util.ObjectUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GetPrimaryHandler implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GetPrimaryHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        String key = params.get("key") != null ? params.get("key").element() : null;

        Map<String, Object> response = new HashMap<>();

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");

        String instanceId = null;

        try {
            ServiceState.PrimaryData primaryData = ServiceState.getPrimaryData(key);
            instanceId = primaryData.getInstanceId();

        } catch (Exception error) {
            LOGGER.error("Get primary instance failed", error);
        }

        if (instanceId == null) {
            response.put("instanceId", "null");
        } else {
            response.put("instanceId", instanceId);
        }

        exchange.getResponseSender().send(ObjectUtils.toJson(response, true));
        exchange.endExchange();
    }
}
