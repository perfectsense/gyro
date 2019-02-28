package gyro.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceQueryGroup {

    private final List<ResourceQuery<Resource>> resourceQueries = new ArrayList<>();

    private ResourceQuery<Resource> apiQuery;

    public List<ResourceQuery<Resource>> getResourceQueries() {
        return resourceQueries;
    }

    public ResourceQueryGroup join(ResourceQueryGroup right) {
        ResourceQueryGroup group = new ResourceQueryGroup();
        group.getResourceQueries().addAll(getResourceQueries());
        group.getResourceQueries().addAll(right.getResourceQueries());
        return group;
    }

    public void merge() {
        List<ResourceQuery<Resource>> queries = new ArrayList<>(resourceQueries);
        ResourceQuery<Resource> first = null;
        Iterator<ResourceQuery<Resource>> iterator = queries.iterator();
        while (iterator.hasNext()) {
            ResourceQuery<Resource> other = iterator.next();
            if (first == null) {
                if (other.apiQuery()) {
                    first = other;
                }
            } else {
                if (first.merge(other)) {
                    iterator.remove();
                }
            }
        }

        resourceQueries.clear();
        resourceQueries.addAll(queries);
        apiQuery = first;
    }

    public List<Resource> query() {
        List<Resource> resources = apiQuery != null ? apiQuery.query() : new ArrayList<>();
        for (ResourceQuery<Resource> resourceQuery : resourceQueries) {
            if (!resourceQuery.apiQuery()) {
                resources = resourceQuery.filter(resources);
            }
        }

        return resources;
    }
}
