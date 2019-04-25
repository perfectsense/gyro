package gyro.lang.ast.condition;

import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class ComparisonConditionNode extends ConditionNode {

    private String operator;

    public ComparisonConditionNode(GyroParser.ComparisonConditionContext context) {
        super(context);

        operator = context.comparisonOperator().getText();
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Object leftValue = getLeftNode().evaluate(scope);
        Object rightValue = getRightNode().evaluate(scope);

        switch (operator) {
            case "==" : return leftValue.equals(rightValue);
            case "!=" : return !leftValue.equals(rightValue);
            default   : return false;
        }
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" ");
        builder.append(operator);
        builder.append(" ");
        builder.append(getRightNode());
    }
}
