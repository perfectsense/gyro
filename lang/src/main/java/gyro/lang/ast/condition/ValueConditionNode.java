package gyro.lang.ast.condition;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class ValueConditionNode extends Node {

    private final Node value;

    public ValueConditionNode(Node value) {
        this.value = Preconditions.checkNotNull(value);
    }

    public ValueConditionNode(GyroParser.ValueConditionContext context) {
        this(Node.create(Preconditions.checkNotNull(context).value()));
    }

    public Node getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitValueCondition(this, context);
    }

}
