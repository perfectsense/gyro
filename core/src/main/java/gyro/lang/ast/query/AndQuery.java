package gyro.lang.ast.query;

import gyro.core.resource.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class AndQuery extends AbstractCompoundQuery {

    public AndQuery(GyroParser.AndQueryContext context) {
        super(context);
    }

    public AndQuery(List<Query> children) {
        super(children);
    }

    @Override
    public List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        for (Query child : getChildren()) {
            resources = child.evaluate(type, scope, resources);
        }

        return resources;
    }
}
