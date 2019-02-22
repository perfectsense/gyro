package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.List;

public class QueryAndNode extends QueryExpressionNode {

    public QueryAndNode(BeamParser.FilterExpressionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Resource resource, List<Resource> resources) throws Exception {
        if (resources == null) {
            return evaluate(resource.scope());
        }

        return null;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        getLeftQueryExpressionNode().setResource(getResource());
        List<Resource> leftValue = (List<Resource>) getLeftQueryExpressionNode().evaluate(scope);

        getRightQueryExpressionNode().setResource(getResource());
        return getRightQueryExpressionNode().evaluate(getResource(), leftValue);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" and ");
        builder.append(getRightNode());
    }

}
