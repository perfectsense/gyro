package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.ArrayList;
import java.util.List;

public class OrQuery extends QueryExpression {

    public OrQuery(QueryExpressionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Resource resource, List<Resource> resources, Scope scope) throws Exception {
        List<Resource> leftValue = (List<Resource>) getLeftQuery().evaluate(resource, resources, scope);
        List<Resource> rightValue = (List<Resource>) getRightQuery().evaluate(resource, resources, scope);

        List<Resource> both = new ArrayList<>();
        both.addAll(leftValue);
        both.addAll(rightValue);

        return both;
    }
}
