package beam.handlers;

import com.psddev.dari.util.ObjectUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.*;

public class InstanceHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        String instanceId = params.get("instanceId") != null ? params.get("instanceId").element() : null;

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> responses = new ArrayList<>();

        if (instanceId != null && ServiceState.getServiceInstance(instanceId) != null) {
            ServiceInstance instance = ServiceState.getServiceInstance(instanceId);
            responses.add(generateResponse(instance));
        } else {
            for (ServiceInstance si : ServiceState.getServiceInstances()) {
                responses.add(generateResponse(si));
            }
        }

        response.put("status", "ok");
        response.put("instances", responses);

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
        exchange.getResponseSender().send(ObjectUtils.toJson(response, true));
    }

    public Map<String, Object> generateResponse(ServiceInstance instance) {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "ok");
        response.put("id", instance.getId());
        response.put("services", instance.getServices());
        response.put("servicesInfo", instance.getServiceInfo());
        response.put("layer", instance.getLayer());
        response.put("location", instance.getLocation());
        response.put("environment", instance.getEnvironment());
        response.put("privateIp", instance.getPrivateIpAddress());
        response.put("mark", instance.getMark().toString());

        return response;
    }

}