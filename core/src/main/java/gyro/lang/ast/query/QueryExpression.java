package gyro.lang.ast.query;

import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.List;

public class QueryExpression extends Query {

    private Query leftQuery;
    private Query rightQuery;

    public QueryExpression(BeamParser.QueryExpressionContext context) {
        this.leftQuery = Query.create(context.getChild(0));
        this.rightQuery = Query.create(context.getChild(2));
    }

    public Query getLeftQuery() {
        return leftQuery;
    }

    public Query getRightQuery() {
        return rightQuery;
    }

    @Override
    public List<ResourceQueryGroup> evaluate(Scope scope, String type, boolean external) throws Exception {
        return null;
    }

}
