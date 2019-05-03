package gyro.lang.ast.condition;

import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class AndConditionNode extends ConditionNode {

    public AndConditionNode(GyroParser.AndConditionContext context) {
        super(context);
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitAndCondition(this, context);
    }

}
