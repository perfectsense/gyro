package gyro.lang.ast.condition;

import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class OrConditionNode extends ConditionNode {

    public OrConditionNode(GyroParser.OrConditionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Boolean leftValue = toBoolean(getLeftNode().evaluate(scope));
        Boolean rightValue = toBoolean(getRightNode().evaluate(scope));

        return leftValue || rightValue;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" or ");
        builder.append(getRightNode());
    }

}
