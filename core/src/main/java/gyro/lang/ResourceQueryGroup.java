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
        // TODO: need to find the first api query and start merging from there
        List<ResourceQuery<Resource>> queries = new ArrayList<>(resourceQueries);
        ResourceQuery<Resource> first = null;
        Iterator<ResourceQuery<Resource>> iterator = queries.iterator();
        while (iterator.hasNext()) {
            ResourceQuery<Resource> other = iterator.next();
            if (first == null) {
                first = other;
            } else {
                if (first.merge(other)) {
                    iterator.remove();
                }
            }
        }

        resourceQueries.clear();
        resourceQueries.addAll(queries);
        apiQuery = resourceQueries.stream().filter(ResourceQuery::apiQuery).findFirst().orElse(null);
    }

    public List<Resource> query() {
        List<Resource> resources = apiQuery != null ? apiQuery.query() : new ArrayList<>();
        for (ResourceQuery<Resource> resourceQuery : resourceQueries) {
            if (!resourceQuery.apiQuery()) {
                resources = resourceQuery.filter(resources);
            }
        }

        resources.stream().forEach(r -> System.out.println(r.toDisplayString()));
        return resources;
    }

    public String toString() {
        return resourceQueries.stream().map(ResourceQuery::toDisplayString).collect(Collectors.toList()).toString();
    }
}
