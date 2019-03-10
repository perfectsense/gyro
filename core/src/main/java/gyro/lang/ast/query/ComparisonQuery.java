package gyro.lang.ast.query;

import gyro.core.BeamException;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.lang.Resource;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryComparisonExpressionContext;

import java.util.List;
import java.util.Objects;

public class ComparisonQuery extends Query {

    private String operator;
    private String fieldName;
    private Node value;

    public static String EQUALS_OPERATOR = "=";
    public static String NOT_EQUALS_OPERATOR = "!=";

    public ComparisonQuery(QueryComparisonExpressionContext context) {
        this.operator = context.operator().getText();
        this.fieldName = context.queryField().getText();
        this.value = Node.create(context.queryValue().getChild(0));
    }

    @Override
    public void evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        Object comparisonValue = value.evaluate(scope);

        if (resources.isEmpty()) {
            return;
        }

        Class<? extends Resource> resourceClass = resources.get(0).getClass();
        boolean validQuery = false;
        for (DiffableField field : DiffableType.getInstance(resourceClass).getFields()) {
            String key = field.getBeamName();
            if (key.equals(fieldName)) {
                validQuery = true;
            }
        }

        if (!validQuery) {
            throw new BeamException(String.format(
                "No such field [%s] defined %s!",
                fieldName, type));
        }

        if (EQUALS_OPERATOR.equals(operator)) {
            resources.removeIf(r -> !Objects.equals(
                DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName).getValue(r), comparisonValue));

        } else if (NOT_EQUALS_OPERATOR.equals(operator)) {
            resources.removeIf(r -> Objects.equals(
                DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName).getValue(r), comparisonValue));

        } else {
            throw new UnsupportedOperationException(String.format("Operator %s is not supported!", operator));
        }
    }
}
