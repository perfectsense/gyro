package gyro.lang.ast.query;

import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.ArrayList;
import java.util.List;

public class OrQuery extends QueryExpression {

    public OrQuery(QueryExpressionContext context) {
        super(context);
    }

    @Override
    public List<ResourceQueryGroup> evaluate(Scope scope, ResourceReferenceNode node) throws Exception {
        List<ResourceQueryGroup> leftQueries = getLeftQuery().evaluate(scope, node);
        List<ResourceQueryGroup> rightQueries = getRightQuery().evaluate(scope, node);
        List<ResourceQueryGroup> result = new ArrayList<>();
        result.addAll(leftQueries);
        result.addAll(rightQueries);

        return result;
    }
}
