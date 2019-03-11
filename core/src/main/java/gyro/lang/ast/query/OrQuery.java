package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.ArrayList;
import java.util.List;

public class OrQuery extends AbstractCompoundQuery {


    public OrQuery(QueryExpressionContext context) {
        super(context);
    }

    @Override
    public void evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        List<Resource> leftResources = new ArrayList<>(resources);
        List<Resource> rightResources = new ArrayList<>(resources);
        getLeft().evaluate(type, scope, leftResources);
        getRight().evaluate(type, scope, rightResources);
        resources.clear();

        resources.addAll(leftResources);
        resources.addAll(rightResources);

        if (getLeft() instanceof CompoundQuery) {
            getChildren().addAll(((CompoundQuery) getLeft()).getChildren());
        } else {
            getChildren().add(getLeft());
        }

        if (getRight() instanceof CompoundQuery) {
            getChildren().addAll(((CompoundQuery) getRight()).getChildren());
        } else {
            getChildren().add(getRight());
        }
    }
}
