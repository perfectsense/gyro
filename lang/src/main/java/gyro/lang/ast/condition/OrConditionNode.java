package gyro.lang.ast.condition;

import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class OrConditionNode extends ConditionNode {

    public OrConditionNode(GyroParser.OrConditionContext context) {
        super(context);
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitOrCondition(this, context);
    }

}
