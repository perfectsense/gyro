package gyro.lang.ast.query;

import gyro.core.query.QueryField;
import gyro.core.query.QueryType;
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
        for (QueryField field : QueryType.getInstance(resourceQuery.getClass()).getFields()) {
            String key = field.getBeamName();
            if (fieldName.equals(key)) {
                field.setValue(resourceQuery, comparisonValue);
                resourceQuery.operator(operator);
                if (EQUALS_OPERATOR.equals(operator) && field.getFilterName() != null) {
                    resourceQuery.apiQuery(true);
                }
            }
        }

        ResourceQueryGroup group = new ResourceQueryGroup();
        group.getResourceQueries().add(resourceQuery);
        return Arrays.asList(group);
    }
}
