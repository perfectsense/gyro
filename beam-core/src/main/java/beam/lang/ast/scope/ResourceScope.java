package beam.lang.ast.scope;

import java.util.HashMap;
import java.util.Map;

import beam.lang.ast.Node;

public class ResourceScope extends Scope {

    private final Map<String, ValueScope> valueScopes = new HashMap<>();

    public ResourceScope(Scope parent) {
        super(parent);
    }

    public void add(String key, Node value, Scope scope) {
        valueScopes.put(key, new ValueScope(value, scope));
    }

    public Map<String, Object> resolve() throws Exception {
        Map<String, Object> resolved = new HashMap<>();

        for (Map.Entry<String, ValueScope> entry : valueScopes.entrySet()) {
            ValueScope vs = entry.getValue();
            resolved.put(entry.getKey(), vs.value.evaluate(vs.scope));
        }

        return resolved;
    }

    private static final class ValueScope {

        public final Node value;
        public final Scope scope;

        public ValueScope(Node value, Scope scope) {
            this.value = value;
            this.scope = scope;
        }
    }
}
