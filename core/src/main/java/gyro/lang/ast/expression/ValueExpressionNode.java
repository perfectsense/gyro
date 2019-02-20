package gyro.lang.ast.expression;

import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

public class ValueExpressionNode extends ExpressionNode {

    public ValueExpressionNode(BeamParser.FilterExpressionContext context) {
        super(context);
    }

    public ValueExpressionNode(BeamParser.ExpressionContext context) {
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
