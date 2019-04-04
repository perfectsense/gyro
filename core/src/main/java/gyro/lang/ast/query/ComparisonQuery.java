package gyro.lang.ast.query;

import gyro.core.BeamException;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.query.QueryField;
import gyro.core.query.QueryType;
import gyro.lang.Resource;
import gyro.lang.ResourceFinder;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ComparisonQuery extends Query {

    private final String operator;
    private final String fieldName;
    private final Node value;

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public ComparisonQuery(BeamParser.ComparisonQueryContext context) {
        this.operator = context.comparisonOperator().getText();
        this.fieldName = context.field().getText();
        this.value = Node.create(context.value().getChild(0));
    }

    public boolean isSupported(ResourceFinder finder) {
        String mapFieldName = fieldName.split("\\.")[0];
        for (QueryField field : QueryType.getInstance(finder.getClass()).getFields()) {
            String key = field.getBeamName();
            if (fieldName.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)
                 || fieldName.contains(".") && mapFieldName.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, String> getFilter(Scope scope) throws Exception {
        Object comparisonValue = value.evaluate(scope);
        Map<String, String> filter = new HashMap<>();
        if (comparisonValue != null) {
            if (fieldName.contains(".")) {
                String mapFieldName = fieldName.split("\\.")[0];
                String mapKey = fieldName.replaceFirst(mapFieldName + ".", "");
                filter.put(String.format("%s:%s", mapFieldName, mapKey), comparisonValue.toString());
            } else {
                filter.put(fieldName, comparisonValue.toString());
            }
        }

        return filter;
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
