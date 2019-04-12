package gyro.core.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gyro.lang.ast.Node;

public class DiffableScope extends Scope {

    public DiffableScope(Scope parent) {
        super(parent);
    }

    public Set<String> getAddedKeys() {
        return getValueNodes().keySet();
    }

    public Map<String, Object> resolve() throws Exception {
        Map<String, Object> resolved = new HashMap<>();

        for (Map.Entry<String, Node> entry : getValueNodes().entrySet()) {
            Node node = entry.getValue();
            resolved.put(entry.getKey(), node.evaluate(this));
        }

        return resolved;
    }
}
