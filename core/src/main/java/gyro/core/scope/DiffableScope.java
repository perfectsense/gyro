package gyro.core.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import gyro.lang.ast.Node;
import gyro.lang.ast.block.BlockNode;

public class DiffableScope extends Scope {

    private final BlockNode block;
    private List<Node> stateNodes = new ArrayList<>();

    public DiffableScope(Scope parent, BlockNode block) {
        super(parent);
        this.block = block;
    }

    public BlockNode getBlock() {
        if (block != null) {
            return block;

        } else {
            return Optional.ofNullable(getParent())
                .map(p -> p.getClosest(DiffableScope.class))
                .map(DiffableScope::getBlock)
                .orElse(null);
        }
    }

    public List<Node> getStateNodes() {
        return stateNodes;
    }

    public void setStateNodes(List<Node> stateNodes) {
        this.stateNodes = stateNodes;
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
