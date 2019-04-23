package gyro.lang.ast.condition;

import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class ValueConditionNode extends ConditionNode {

    public ValueConditionNode(GyroParser.ValueConditionContext context, String file) {
        super(context, file);
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
