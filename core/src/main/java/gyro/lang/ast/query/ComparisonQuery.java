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
import java.util.stream.Collectors;

public class ComparisonQuery extends Query {

    private final String operator;
    private final String fieldName;
    private final Node value;

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public ComparisonQuery(QueryComparisonExpressionContext context) {
        this.operator = context.operator().getText();
        this.fieldName = context.queryField().getText();
        this.value = Node.create(context.queryValue().getChild(0));
    }

    @Override
    public List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        Object comparisonValue = value.evaluate(scope);

        for (Resource resource : resources) {
            Class<? extends Resource> resourceClass = resource.getClass();
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
                    fieldName, resourceClass));
            }
        }

        if (EQUALS_OPERATOR.equals(operator)) {
            return resources.stream()
                .filter(r -> Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName).getValue(r), comparisonValue))
                .collect(Collectors.toList());

        } else if (NOT_EQUALS_OPERATOR.equals(operator)) {
            return resources.stream()
                .filter(r -> !Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName).getValue(r), comparisonValue))
                .collect(Collectors.toList());

        } else {
            throw new UnsupportedOperationException(String.format("Operator %s is not supported!", operator));
        }
    }
}
