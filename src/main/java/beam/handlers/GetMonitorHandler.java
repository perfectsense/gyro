package beam.handlers;

import com.psddev.dari.util.ObjectUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GetMonitorHandler implements HttpHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GetMonitorHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Deque<String>> params = exchange.getQueryParameters();

        String service = params.get("service") != null ? params.get("service").element() : null;
        String type = params.get("type") != null ? params.get("type").element() : null;

        Map<String, Object> response = ServiceState.getMonitor();

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");

        if (service == null || type == null) {
            exchange.getResponseSender().send(ObjectUtils.toJson(response, true));

        } else if (service.equals("mysql") && type.equals("healthy")) {
            Map<String, Long> serviceMap = (Map<String, Long>) response.get("SERVICE");
            exchange.getResponseSender().send(serviceMap.get("mysql").toString());

        } else if (service.equals("solr") && type.equals("healthy")) {
            Map<String, Long> serviceMap = (Map<String, Long>) response.get("SERVICE");
            exchange.getResponseSender().send(serviceMap.get("solr").toString());

        } else if (service.equals("reader") && type.equals("healthy")) {
            Map<String, Long> serviceMap = (Map<String, Long>) response.get("SERVICE");
            exchange.getResponseSender().send(serviceMap.get("reader").toString());

        } else if (service.equals("service-discovery") && type.equals("replication")) {
            Map<String, Long> replicationMap = (Map<String, Long>) response.get("REPLICATION");

            long totalReplication = 0;
            for (Long count : replicationMap.values()) {
                totalReplication += count;
            }

            exchange.getResponseSender().send(Long.toString(totalReplication));

        } else if (service.equals("service-discovery") && type.equals("host")) {
            Map<String, Long> hostMap = (Map<String, Long>) response.get("HOST");
            exchange.getResponseSender().send(hostMap.get("host").toString());
        }

        exchange.endExchange();
    }
}
