package gyro.lang.query;

import gyro.core.resource.Resource;
import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class AndQuery extends AbstractCompoundQuery {

    public AndQuery(GyroParser.AndQueryContext context, String file) {
        super(context, file);
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
