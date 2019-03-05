package gyro.lang;

import java.util.ArrayList;
import java.util.List;

public class ResourceQueryGroup {

    private final List<ResourceQuery<Resource>> resourceQueries = new ArrayList<>();

    private final ExternalResourceQuery<Resource> apiQuery;

    public ResourceQueryGroup(ExternalResourceQuery<Resource> query) {
        this.apiQuery = query;
    }

    public List<ResourceQuery<Resource>> getResourceQueries() {
        return resourceQueries;
    }

    public static void join(ResourceQueryGroup left, ResourceQueryGroup right, ResourceQueryGroup result) {
        result.getResourceQueries().addAll(left.getResourceQueries());
        result.getResourceQueries().addAll(right.getResourceQueries());
    }

    public void merge() {
        List<ResourceQuery<Resource>> queries = new ArrayList<>(resourceQueries);
        queries.removeIf(apiQuery::merge);
        resourceQueries.clear();
        resourceQueries.addAll(queries);
    }

    public List<Resource> query() {
        if (apiQuery == null) {
            throw new IllegalStateException();
        }

        List<Resource> resources = apiQuery.query();
        for (ResourceQuery<Resource> resourceQuery : resourceQueries) {
            if (!resourceQuery.external()) {
                resources = resourceQuery.filter(resources);
            }
        }

        return resources;
    }

    public List<Resource> query(List<Resource> baseResources) {
        List<Resource> resources = new ArrayList<>(baseResources);
        for (ResourceQuery<Resource> resourceQuery : resourceQueries) {
            resources = resourceQuery.filter(resources);
        }

        return resources;
    }
}
