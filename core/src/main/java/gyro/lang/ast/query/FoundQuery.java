package gyro.lang.ast.query;

import com.google.common.collect.ImmutableList;
import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;

import java.util.List;

public class FoundQuery extends Query {

    private final List<Resource> resources;

    public FoundQuery(List<Resource> resources) {
        this.resources = ImmutableList.copyOf(resources);
    }

    @Override
    public List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        return this.resources;
    }
}
