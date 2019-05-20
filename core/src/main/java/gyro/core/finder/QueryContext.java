package gyro.core.finder;

import java.util.List;

import gyro.core.resource.Resource;
import gyro.core.resource.Scope;

public class QueryContext {

    private final String type;
    private final Scope scope;
    private final List<Resource> resources;

    public QueryContext(String type, Scope scope, List<Resource> resources) {
        this.type = type;
        this.scope = scope;
        this.resources = resources;
    }

    public String getType() {
        return type;
    }

    public Scope getScope() {
        return scope;
    }

    public List<Resource> getResources() {
        return resources;
    }

}
