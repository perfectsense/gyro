package gyro.core.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gyro.lang.ast.Node;

public class DiffableScope extends Scope {

    private boolean extended;

    public DiffableScope(Scope parent) {
        super(parent);
    }

    public boolean isExtended() {
        return extended;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public Set<String> getAddedKeys() {
        return getValueNodes().keySet();
    }

    public Map<String, Object> resolve() {
        NodeEvaluator evaluator = getRootScope().getEvaluator();
        Map<String, Object> resolved = new HashMap<>();

        for (Map.Entry<String, Node> entry : getValueNodes().entrySet()) {
            Node node = entry.getValue();
            resolved.put(entry.getKey(), evaluator.visit(node, this));
        }

        return resolved;
    }
}
