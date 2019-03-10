package gyro.lang.ast.query;

import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.ArrayList;
import java.util.List;

public class AndQuery extends CompoundQuery {

    private final Query left;
    private final Query right;

    public AndQuery(QueryExpressionContext context) {
        this.left = Query.create(context.getChild(0));
        this.right = Query.create(context.getChild(2));
    }

    public Query getLeft() {
        return left;
    }

    public Query getRight() {
        return right;
    }

    @Override
    public List<ResourceQueryGroup> evaluate(Scope scope, String type, boolean external) throws Exception {
        List<ResourceQueryGroup> leftQueries = getLeft().evaluate(scope, type, external);
        List<ResourceQueryGroup> rightQueries = getRight().evaluate(scope, type, external);
        List<ResourceQueryGroup> result = new ArrayList<>();
        for (ResourceQueryGroup left : leftQueries) {
            for (ResourceQueryGroup right : rightQueries) {
                ResourceQueryGroup joined = new ResourceQueryGroup(Query.createExternalResourceQuery(scope, type));
                ResourceQueryGroup.join(left, right, joined);
                result.add(joined);
            }
        }

        return result;
    }
}
