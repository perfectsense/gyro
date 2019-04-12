package gyro.lang.ast.query;

import gyro.core.resource.Resource;
import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.LinkedList;
import java.util.List;

public class OrQuery extends AbstractCompoundQuery {

    public OrQuery(GyroParser.OrQueryContext context) {
        super(context);
    }

    public OrQuery(List<Query> children) {
        super(children);
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
