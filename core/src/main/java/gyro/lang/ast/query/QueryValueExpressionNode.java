package gyro.lang.ast.query;

import gyro.lang.ast.scope.Scope;
import gyro.lang.query.QueryFilter;
import gyro.parser.antlr4.BeamParser;

public class QueryValueExpressionNode extends QueryExpressionNode {

    @Override
    public QueryFilter toFilter(Scope scope) {
        return null;
    }

    public QueryValueExpressionNode(BeamParser.FilterExpressionContext context) {
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
