package gyro.core.reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gyro.core.GyroException;
import gyro.core.finder.Finder;
import gyro.core.finder.FinderSettings;
import gyro.core.finder.FinderType;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.Scope;
import gyro.lang.query.AndQuery;
import gyro.lang.query.ComparisonQuery;
import gyro.lang.query.Query;

public class FinderReferenceResolver extends ReferenceResolver {

    @Override
    public String getName() {
        return "external-query";
    }

    @Override
    public Object resolve(Scope scope, List<Object> arguments, List<Query> queries) {
        String type = (String) arguments.remove(0);

        Class<? extends Finder<Resource>> finderClass = scope.getRootScope()
            .getSettings(FinderSettings.class)
            .getFinderClasses()
            .get(type);

        if (finderClass == null) {
            throw new GyroException(String.format(
                "[%s] resource doesn't support external queries!",
                type));
        }

        Finder<Resource> finder = FinderType.getInstance(finderClass).newInstance(scope);
        List<Resource> resources = null;

        if (arguments.isEmpty() && !queries.isEmpty()) {
            Map<String, String> filters = getFilters(scope, queries);

            if (!filters.isEmpty()) {
                resources = finder.find(filters);
            }
        }

        if (resources == null) {
            resources = finder.findAll();
        }

        return ReferenceResolver.resolveRemaining(
            scope,
            arguments,
            queries,
            resources);
    }

    private Map<String, String> getFilters(Scope scope, List<Query> queries) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Map<String, String> filters = new HashMap<>();

        for (Iterator<Query> i = queries.iterator(); i.hasNext(); ) {
            Query query = i.next();

            if (query instanceof AndQuery) {
                List<Query> childQueries = new ArrayList<>(((AndQuery) query).getChildren());
                Map<String, String> childFilters = getFilters(scope, childQueries);

                if (childQueries.isEmpty() && !childFilters.isEmpty()) {
                    filters.putAll(childFilters);
                    i.remove();
                }

            } else if (query instanceof ComparisonQuery) {
                ComparisonQuery comparison = (ComparisonQuery) query;

                if ("=".equals(comparison.getOperator())) {
                    filters.put(comparison.getKey(), (String) evaluator.visit(comparison.getValue(), scope));
                    i.remove();
                }
            }
        }

        return filters;
    }

}
