package gyro.lang.ast.expression;

import gyro.lang.ast.scope.Scope;
import gyro.lang.query.EqualsQueryFilter;
import gyro.lang.query.NotEqualsFilter;
import gyro.lang.query.QueryFilter;
import gyro.parser.antlr4.BeamParser.ComparisonExpressionContext;
import gyro.parser.antlr4.BeamParser.FilterComparisonExpressionContext;
import gyro.parser.antlr4.BeamParser.FilterExpressionContext;

import static gyro.parser.antlr4.BeamParser.ExpressionContext;

public class ComparisonNode extends ExpressionNode {

    private String operator;

    @Override
    public QueryFilter toFilter(Scope scope) {
        try {
            Object leftValue = getLeftNode().evaluate(scope);
            Object rightValue = getRightNode().evaluate(scope);

            switch (operator) {
                case "==":
                    return new EqualsQueryFilter(leftValue.toString(), rightValue.toString());
                case "!=":
                    return new NotEqualsFilter(leftValue.toString(), rightValue.toString());
            }
        } catch (Exception ex) {
            // TODO: ??
        }

        throw new IllegalStateException();
    }

    public ComparisonNode(FilterExpressionContext context) {
        super(context);

        FilterComparisonExpressionContext compareContext = (FilterComparisonExpressionContext) context;
        operator = compareContext.operator().getText();
    }

    public ComparisonNode(ExpressionContext context) {
        super(context);

        ComparisonExpressionContext compareContext = (ComparisonExpressionContext) context;
        operator = compareContext.operator().getText();
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Object leftValue = getLeftNode().evaluate(scope);
        Object rightValue = getRightNode().evaluate(scope);

        switch (operator) {
            case "==" : return leftValue.equals(rightValue);
            case "!=" : return !leftValue.equals(rightValue);
            default   : return false;
        }
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" ");
        builder.append(operator);
        builder.append(" ");
        builder.append(getRightNode());
    }
}
