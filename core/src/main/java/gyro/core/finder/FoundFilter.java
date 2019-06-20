package gyro.core.finder;

import java.util.List;

import com.google.common.collect.ImmutableList;
import gyro.core.resource.Resource;
import gyro.lang.filter.Filter;
import gyro.lang.filter.FilterVisitor;

public class FoundFilter extends Filter {

    private final List<Resource> resources;

    public FoundFilter(List<Resource> resources) {
        this.resources = ImmutableList.copyOf(resources);
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, R> R accept(FilterVisitor<C, R> visitor, C context) {
        return (R) resources;
    }

}
