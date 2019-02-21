package gyro.lang.ast.query;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.lang.query.OrQueryFilter;
import gyro.lang.query.QueryFilter;
import gyro.parser.antlr4.BeamParser;

public class QueryOrNode extends QueryExpressionNode {

    @Override
    public QueryFilter toFilter(Scope scope) {
        Node leftNode = getLeftNode();
        if (!(leftNode instanceof QueryComparisonNode)) {
            // TODO: Beam Exception with line number information...
            throw new IllegalStateException();
        }

        Node rightNode = getRightNode();
        if (!(rightNode instanceof QueryComparisonNode)) {
            // TODO: Beam Exception with line number information...
            throw new IllegalStateException();
        }

        return new OrQueryFilter(
            ((QueryComparisonNode) leftNode).toFilter(scope),
            ((QueryComparisonNode) rightNode).toFilter(scope)
        );
    }

    public QueryOrNode(BeamParser.FilterExpressionContext context) {
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
