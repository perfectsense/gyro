package beam.lang.ast.types;

import beam.lang.ast.ExpressionNode;
import beam.lang.ast.Scope;
import beam.parser.antlr4.BeamParser;

public class ValueExpressionNode extends ExpressionNode {

    public ValueExpressionNode(BeamParser.ExpressionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Scope scope) {
        return getLeftNode().evaluate(scope);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
    }

}
