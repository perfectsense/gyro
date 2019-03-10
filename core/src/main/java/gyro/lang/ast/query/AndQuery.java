package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.Collections;
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
    public void evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        getLeft().evaluate(type, scope, resources);
        getRight().evaluate(type, scope, resources);

        List<Query> leftQueries;
        List<Query> rightQueries;

        if (getLeft() instanceof CompoundQuery) {
            leftQueries = ((CompoundQuery) getLeft()).getChildren();
        } else {
            leftQueries = Collections.singletonList(getLeft());
        }

        if (getRight() instanceof CompoundQuery) {
            rightQueries = ((CompoundQuery) getRight()).getChildren();
        } else {
            rightQueries = Collections.singletonList(getRight());
        }

        for (Query left : leftQueries) {
            for (Query right : rightQueries) {
                CompoundQuery joined = new CompoundQuery();
                joined.getChildren().add(left);
                joined.getChildren().add(right);
                getChildren().add(joined);
            }
        }
    }
}
