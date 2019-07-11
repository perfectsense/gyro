package gyro.core.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gyro.lang.ast.Node;

public class DiffableScope extends Scope {

    private List<Node> stateNodes = new ArrayList<>();

    private boolean extended;

    public DiffableScope(Scope parent) {
        super(parent);
    }

    public List<Node> getStateNodes() {
        return stateNodes;
    }

    public void setStateNodes(List<Node> stateNodes) {
        this.stateNodes = stateNodes;
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
