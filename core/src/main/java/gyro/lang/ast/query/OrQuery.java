package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.LinkedList;
import java.util.List;

public class OrQuery extends AbstractCompoundQuery {


    public OrQuery(QueryExpressionContext context) {
        super(context);
    }

    @Override
    public List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        List<Resource> joined = new LinkedList<>();
        for (Query child : getChildren()) {
            joined.addAll(child.evaluate(type, scope, resources));
        }

        return joined;
    }
}
