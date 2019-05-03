package gyro.lang.ast.condition;

import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class OrConditionNode extends ConditionNode {

    public OrConditionNode(GyroParser.OrConditionContext context) {
        super(context);
    }

    @Override
    public <C> Object accept(NodeVisitor<C> visitor, C context) {
        return visitor.visitOrCondition(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" or ");
        builder.append(getRightNode());
    }

}
