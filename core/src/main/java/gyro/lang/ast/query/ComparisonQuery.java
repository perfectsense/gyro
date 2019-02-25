package gyro.lang.ast.query;

import gyro.core.diff.DiffableType;
import gyro.lang.Resource;
import gyro.lang.ResourceQuery;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryComparisonExpressionContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    public Object evaluate(Resource resource, List<Resource> resources, Scope scope) throws Exception {
        Object comparisonValue = value.evaluate(scope);

        // Fetch resources from API.
        if (resources == null) {
            if (EQUALS_OPERATOR.equals(operator)) {
                Map<String, String> filter = new HashMap<>();
                filter.put(fieldName, comparisonValue.toString());

                ResourceQuery resourceQuery = (ResourceQuery) resource;
                List<Resource> results = resourceQuery.query(filter);

                if (results == null) {
                    results = resourceQuery.queryAll();
                    return evaluate(resource, results, scope);
                }

                return results;
            } else if (NOT_EQUALS_OPERATOR.equals(operator)) {
                ResourceQuery resourceQuery = (ResourceQuery) resource;
                return evaluate(resource, resourceQuery.queryAll(), scope);
            }
        } else {
            for (Iterator<Resource> i = resources.iterator(); i.hasNext();) {
                Resource r = i.next();

                boolean equal = Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName).getValue(r),
                    comparisonValue);

                if (EQUALS_OPERATOR.equals(operator) && !equal) {
                    i.remove();
                } else if (NOT_EQUALS_OPERATOR.equals(operator) && equal) {
                    i.remove();
                }
            }

            return resources;
        }

        return null;
    }
}
