package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;

import java.util.ArrayList;
import java.util.List;

public class CompoundQuery extends Query {

    private final List<Query> children = new ArrayList<>();

    public List<Query> getChildren() {
        return children;
    }

    @Override
    public void evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        throw new UnsupportedOperationException();
    }
}
