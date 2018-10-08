package beam.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeamContext {

    private Map<String, Object> context;

    public Map<String, Object> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }

        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public BeamContext scopeContext(String scope) {
        BeamContext scopeContext = new BeamContext();
        for (String key : getContext().keySet()) {
            String scopeKey = String.format("%s.%s", scope, key);
            Object value = getContext().get(key);
            if (value instanceof Map) {
                scopeContext.getContext().put(scopeKey, scopeMap((Map) value, scope));
            } else if (value instanceof List) {
                scopeContext.getContext().put(scopeKey, scopeList((List) value, scope));
            } else if (value instanceof BeamReference) {
                String newReferenceKey = String.format("%s.%s", scope, ((BeamReference) value).getKey());
                scopeContext.getContext().put(scopeKey, new BeamReference(newReferenceKey));
            } else {
                scopeContext.getContext().put(scopeKey, value);
            }
        }

        return scopeContext;
    }

    private Map scopeMap(Map map, String scope) {
        Map result = new HashMap<>();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {
                result.put(key, scopeMap((Map) value, scope));
            } else if (value instanceof List) {
                result.put(key, scopeList((List) value, scope));
            } else if (value instanceof BeamReference) {
                String newReferenceKey = String.format("%s.%s", scope, ((BeamReference) value).getKey());
                result.put(key, new BeamReference(newReferenceKey));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    private List scopeList(List list, String scope) {
        List result = new ArrayList();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(scopeMap((Map) item, scope));
            } else if (item instanceof List) {
                result.add(scopeList((List) item, scope));
            } else if (item instanceof BeamReference) {
                String newReferenceKey = String.format("%s.%s", scope, ((BeamReference) item).getKey());
                result.add(new BeamReference(newReferenceKey));
            } else {
                result.add(item);
            }
        }

        return result;
    }
}
