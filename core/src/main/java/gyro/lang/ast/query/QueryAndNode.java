package gyro.lang.ast.query;

import gyro.lang.ast.scope.Scope;
import gyro.lang.query.QueryFilter;
import gyro.parser.antlr4.BeamParser;

public class QueryAndNode extends QueryExpressionNode {

    @Override
    public QueryFilter toFilter(Scope scope) {
        return null;
    }

    public QueryAndNode(BeamParser.FilterExpressionContext context) {
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
