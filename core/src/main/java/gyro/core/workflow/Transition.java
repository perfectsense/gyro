package gyro.core.workflow;

import gyro.core.scope.NodeEvaluator;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.ResourceNode;
import gyro.core.scope.Scope;

public class Transition {

    private final String name;
    private final String to;
    private final String description;

    public Transition(Scope parent, ResourceNode node) {
        Scope scope = new Scope(parent);
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        for (Node item : node.getBody()) {
            evaluator.visit(item, scope);
        }

        name = (String) evaluator.visit(node.getNameNode(), parent);
        to = (String) scope.get("to");
        description = (String) scope.get("description");
    }

    public String getTo() {
        return to;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
