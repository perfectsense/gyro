package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.List;

public class AndQuery extends QueryExpression {

    public AndQuery(QueryExpressionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Resource resource, List<Resource> resources, Scope scope) throws Exception {
        List<Resource> leftValue = (List<Resource>) getLeftQuery().evaluate(resource, resources, scope);
        return getRightQuery().evaluate(resource, leftValue, scope);
    }

}
