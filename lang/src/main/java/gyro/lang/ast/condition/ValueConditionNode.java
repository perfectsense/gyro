package gyro.lang.ast.condition;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;

public class ValueConditionNode extends Node {

    private final ValueNode value;

    public ValueConditionNode(ValueNode value) {
        this.value = Preconditions.checkNotNull(value);
    }

    public ValueConditionNode(GyroParser.ValueConditionContext context) {
        this((ValueNode) Node.create(Preconditions.checkNotNull(context).value()));
    }

    public ValueNode getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitValueCondition(this, context);
    }

}
