package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.List;

public class QueryValueExpressionNode extends QueryExpressionNode {

    public QueryValueExpressionNode(BeamParser.FilterExpressionContext context) {
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
        return getLeftNode().evaluate(scope);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
    }

}
