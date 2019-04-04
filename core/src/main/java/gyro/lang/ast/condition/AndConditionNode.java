package gyro.lang.ast.condition;

import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class AndConditionNode extends ConditionNode {

    public AndConditionNode(GyroParser.AndConditionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Boolean leftValue = toBoolean(getLeftNode().evaluate(scope));
        Boolean rightValue = toBoolean(getRightNode().evaluate(scope));

        return leftValue && rightValue;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" and ");
        builder.append(getRightNode());
    }

}
