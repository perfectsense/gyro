package gyro.lang.ast.control;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

public class IfNode extends Node {

    private final List<Node> conditions;
    private final List<List<Node>> bodies;

    public IfNode(List<Node> conditions, List<List<Node>> bodies) {
        this.conditions = ImmutableList.copyOf(Preconditions.checkNotNull(conditions));

        this.bodies = Preconditions.checkNotNull(bodies)
            .stream()
            .map(ImmutableList::copyOf)
            .collect(ImmutableCollectors.toList());
    }

    public IfNode(GyroParser.IfStatementContext context) {
        this(
            Node.create(Preconditions.checkNotNull(context).condition()),

            context.blockBody()
                .stream()
                .map(GyroParser.BlockBodyContext::blockStatement)
                .map(Node::create)
                .collect(ImmutableCollectors.toList()));
    }

    public List<Node> getConditions() {
        return conditions;
    }

    public List<List<Node>> getBodies() {
        return bodies;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitIf(this, context);
    }

}
