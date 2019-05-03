package gyro.lang.query;

import com.google.common.collect.ImmutableList;
import gyro.core.resource.Resource;

import java.util.List;

public class FoundQuery extends Query {

    private final List<Resource> resources;

    public FoundQuery(List<Resource> resources) {
        this.resources = ImmutableList.copyOf(resources);
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    public <C, R> R accept(QueryVisitor<C, R> visitor, C context) {
        return visitor.visitFound(this, context);
    }

}
