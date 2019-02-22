package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;

public class QueryOrNode extends QueryExpressionNode {

    public QueryOrNode(BeamParser.FilterExpressionContext context) {
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
        List<Resource> leftValue = (List<Resource>) getLeftNode().evaluate(scope);
        List<Resource> rightValue = (List<Resource>) getRightNode().evaluate(scope);

        List<Resource> both = new ArrayList<>();
        both.addAll(leftValue);
        both.addAll(rightValue);

        return both;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" or ");
        builder.append(getRightNode());
    }

}
