package gyro.lang.ast.scope;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import gyro.core.diff.Diffable;
import gyro.lang.ast.Node;

public class DiffableScope extends Scope {

    private Set<Diffable> dependencies = new LinkedHashSet<>();
    private Set<Diffable> dependents = new LinkedHashSet<>();

    public DiffableScope(Scope parent) {
        super(parent);
    }

    public Set<String> getAddedKeys() {
        return getValueNodes().keySet();
    }

    public Set<Diffable> dependencies() {
        return dependencies;
    }

    public Set<Diffable> dependents() {
        return dependents;
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
