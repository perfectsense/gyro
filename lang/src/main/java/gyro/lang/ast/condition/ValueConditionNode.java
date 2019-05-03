package gyro.lang.ast.condition;

import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class ValueConditionNode extends ConditionNode {

    public ValueConditionNode(GyroParser.ValueConditionContext context) {
        super(context);
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitValueCondition(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
    }

}
