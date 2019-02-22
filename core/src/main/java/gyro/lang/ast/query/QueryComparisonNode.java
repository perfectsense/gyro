package gyro.lang.ast.query;

import gyro.core.diff.DiffableType;
import gyro.lang.Resource;
import gyro.lang.ResourceQuery;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryComparisonExpressionContext;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QueryComparisonNode extends QueryExpressionNode {

    private String operator;

    public QueryComparisonNode(QueryExpressionContext context) {
        super(context);

        QueryComparisonExpressionContext compareContext = (QueryComparisonExpressionContext) context;
        operator = compareContext.operator().getText();
    }

    @Override
    public Object evaluate(Resource resource, List<Resource> resources) throws Exception {
        if (resources == null) {
            return evaluate(resource.scope());
        }

        String leftValue = (String) getLeftNode().evaluate(resource.scope());
        Object rightValue = getRightNode().evaluate(resource.scope());

        for (Iterator<Resource> i = resources.iterator(); i.hasNext();) {
            Resource r = i.next();

            boolean equal = Objects.equals(
                DiffableType.getInstance(r.getClass()).getFieldByBeamName(leftValue).getValue(r),
                rightValue);

            if ("==".equals(operator) && !equal) {
                i.remove();
            } else if ("!=".equals(operator) && equal) {
                i.remove();
            }
        }

        return resources;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Object leftValue = getLeftNode().evaluate(scope);
        Object rightValue = getRightNode().evaluate(scope);

        if ("==".equals(operator)) {
            Map<String, String> filter = new HashMap<>();
            filter.put(leftValue.toString(), rightValue.toString());

            ResourceQuery resourceQuery = (ResourceQuery) getResource();
            return resourceQuery.query(filter);
        } else if ("!=".equals(operator)) {
            ResourceQuery resourceQuery = (ResourceQuery) getResource();
            return evaluate(getResource(), resourceQuery.queryAll());
        }

        return null;
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
