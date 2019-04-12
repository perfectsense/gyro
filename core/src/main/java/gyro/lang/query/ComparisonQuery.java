package gyro.lang.query;

import gyro.core.GyroException;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.resource.ResourceFinderField;
import gyro.core.resource.ResourceFinderType;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceFinder;
import gyro.lang.ast.Node;
import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ComparisonQuery extends Query {

    private final String operator;
    private final String path;
    private final Node value;

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public ComparisonQuery(GyroParser.ComparisonQueryContext context) {
        this.operator = context.comparisonOperator().getText();
        this.path = context.path().getText();
        this.value = Node.create(context.value().getChild(0));
    }

    public boolean isSupported(ResourceFinder finder) {
        String mapFieldName = path.split("\\.")[0];
        for (ResourceFinderField field : ResourceFinderType.getInstance(finder.getClass()).getFields()) {
            String key = field.getGyroName();
            if (path.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)
                 || path.contains(".") && mapFieldName.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, String> getFilter(Scope scope) throws Exception {
        Object comparisonValue = value.evaluate(scope);
        Map<String, String> filter = new HashMap<>();
        if (comparisonValue != null) {
            if (path.contains(".")) {
                String mapFieldName = path.split("\\.")[0];
                String mapKey = path.replaceFirst(mapFieldName + ".", "");
                filter.put(String.format("%s:%s", mapFieldName, mapKey), comparisonValue.toString());
            } else {
                filter.put(path, comparisonValue.toString());
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
                String key = field.getGyroName();
                if (key.equals(path)) {
                    validQuery = true;
                }
            }

            if (!validQuery) {
                throw new GyroException(String.format(
                    "No such field [%s] defined %s!",
                    path, resourceClass));
            }
        }

        if (EQUALS_OPERATOR.equals(operator)) {
            return resources.stream()
                .filter(r -> Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByGyroName(path).getValue(r), comparisonValue))
                .collect(Collectors.toList());

        } else if (NOT_EQUALS_OPERATOR.equals(operator)) {
            return resources.stream()
                .filter(r -> !Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByGyroName(path).getValue(r), comparisonValue))
                .collect(Collectors.toList());

        } else {
            throw new UnsupportedOperationException(String.format("Operator %s is not supported!", operator));
        }
    }
}
