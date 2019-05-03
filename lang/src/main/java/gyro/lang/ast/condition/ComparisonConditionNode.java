package gyro.lang.ast.condition;

import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class ComparisonConditionNode extends ConditionNode {

    private final String operator;

    public ComparisonConditionNode(GyroParser.ComparisonConditionContext context) {
        super(context);

        operator = context.comparisonOperator().getText();
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitComparisonCondition(this, context);
    }

}