package gyro.core.workflow;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.ResourceNode;

public class Create {

    private final Node type;
    private final Node name;
    private final List<Node> body;

    public Create(Node type, Node name, List<Node> body) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public Node getType() {
        return type;
    }

    public Node getName() {
        return name;
    }

    public List<Node> getBody() {
        return body;
    }

    public void execute(GyroUI ui, State state, RootScope current, RootScope pending, Scope executing) {
        NodeEvaluator evaluator = executing.getRootScope().getEvaluator();

        evaluator.visit(
            new ResourceNode(
                (String) evaluator.visit(type, executing),
                name,
                body),
            executing);
    }

}
