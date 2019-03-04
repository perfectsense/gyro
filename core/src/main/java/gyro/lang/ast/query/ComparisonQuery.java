package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ResourceQuery;
import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
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
    public List<ResourceQueryGroup> evaluate(Scope scope, String type, boolean external) throws Exception {
        Object comparisonValue = value.evaluate(scope);
        ResourceQuery<Resource> resourceQuery = null;
        if (external) {
            resourceQuery = Query.createExternalResourceQuery(scope, type, fieldName, operator, comparisonValue);
        }

        if (resourceQuery == null) {
            resourceQuery = Query.createInternalResourceQuery(scope, type, fieldName, operator, comparisonValue);
        }

        ResourceQueryGroup group = new ResourceQueryGroup(Query.createExternalResourceQuery(scope, type));
        group.getResourceQueries().add(resourceQuery);
        return Arrays.asList(group);
    }
}
