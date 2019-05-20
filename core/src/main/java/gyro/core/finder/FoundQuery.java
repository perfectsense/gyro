package gyro.core.finder;

import java.util.List;

import com.google.common.collect.ImmutableList;
import gyro.core.resource.Resource;
import gyro.lang.query.Query;
import gyro.lang.query.QueryVisitor;

public class FoundQuery extends Query {

    private final List<Resource> resources;

    public FoundQuery(List<Resource> resources) {
        this.resources = ImmutableList.copyOf(resources);
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, R> R accept(QueryVisitor<C, R> visitor, C context) {
        return (R) resources;
    }

}
