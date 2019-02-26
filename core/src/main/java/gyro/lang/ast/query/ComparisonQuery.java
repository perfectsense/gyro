package gyro.lang.ast.query;

import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.lang.Resource;
import gyro.lang.ResourceQuery;
import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.parser.antlr4.BeamParser.QueryComparisonExpressionContext;

import java.util.Arrays;
import java.util.List;

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
    public List<ResourceQueryGroup> evaluate(Scope scope, ResourceReferenceNode node) throws Exception {
        Object comparisonValue = value.evaluate(scope);
        ResourceQuery<Resource> resourceQuery = node.getResourceQuery(scope);
        for (DiffableField field : DiffableType.getInstance(resourceQuery.getClass()).getFields()) {
            String key = field.getBeamName();
            if (fieldName.equals(key)) {
                field.setValue(resourceQuery, comparisonValue);
            }
        }

        ResourceQueryGroup group = new ResourceQueryGroup();
        group.getResourceQueries().add(resourceQuery);
        return Arrays.asList(group);


//        // Fetch resources from API.
//        if (resources == null) {
//            if (EQUALS_OPERATOR.equals(operator)) {
//                Map<String, String> filter = new HashMap<>();
//                filter.put(fieldName, comparisonValue.toString());
//
//                List<Resource> results = resourceQuery.query(filter);
//
//                if (results == null) {
//                    results = resourceQuery.queryAll();
//                    return evaluate(resourceQuery, results, scope);
//                }
//
//                return results;
//            } else if (NOT_EQUALS_OPERATOR.equals(operator)) {
//                return evaluate(resourceQuery, resourceQuery.queryAll(), scope);
//            }
//        } else {
//            for (Iterator<Resource> i = resources.iterator(); i.hasNext();) {
//                Resource r = i.next();
//
//                boolean equal = Objects.equals(
//                    DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName).getValue(r),
//                    comparisonValue);
//
//                if (EQUALS_OPERATOR.equals(operator) && !equal) {
//                    i.remove();
//                } else if (NOT_EQUALS_OPERATOR.equals(operator) && equal) {
//                    i.remove();
//                }
//            }
//
//            return resources;
//        }

        //return null;
    }
}
