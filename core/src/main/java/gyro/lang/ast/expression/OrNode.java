package gyro.lang.ast.expression;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.lang.query.OrQueryFilter;
import gyro.lang.query.QueryFilter;
import gyro.parser.antlr4.BeamParser;

public class OrNode extends ExpressionNode {

    @Override
    public QueryFilter toFilter(Scope scope) {
        Node leftNode = getLeftNode();
        if (!(leftNode instanceof ComparisonNode)) {
            // TODO: Beam Exception with line number information...
            throw new IllegalStateException();
        }

        Node rightNode = getRightNode();
        if (!(rightNode instanceof ComparisonNode)) {
            // TODO: Beam Exception with line number information...
            throw new IllegalStateException();
        }

        return new OrQueryFilter(
            ((ComparisonNode) leftNode).toFilter(scope),
            ((ComparisonNode) rightNode).toFilter(scope)
        );
    }

    public OrNode(BeamParser.FilterExpressionContext context) {
        super(context);
    }

    public OrNode(BeamParser.ExpressionContext context) {
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
