package gyro.lang.ast.query;

import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.List;

public class QueryExpression extends Query {

    private final Query left;
    private final Query right;

    public QueryExpression(BeamParser.QueryExpressionContext context) {
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
        return null;
    }

}
