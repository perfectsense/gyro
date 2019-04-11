package gyro.lang.ast.condition;

import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class ValueConditionNode extends ConditionNode {

    public ValueConditionNode(GyroParser.ValueConditionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        return getLeftNode().evaluate(scope);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
    }

}
