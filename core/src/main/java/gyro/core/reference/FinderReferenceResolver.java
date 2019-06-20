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
import gyro.lang.filter.AndFilter;
import gyro.lang.filter.ComparisonFilter;
import gyro.lang.filter.Filter;

public class FinderReferenceResolver extends ReferenceResolver {

    @Override
    public String getName() {
        return "external-query";
    }

    @Override
    public Object resolve(Scope scope, List<Object> arguments, List<Filter> filters) {
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

        if (arguments.isEmpty() && !filters.isEmpty()) {
            Map<String, String> finderFilters = getFilters(scope, filters);

            if (!filters.isEmpty()) {
                resources = finder.find(finderFilters);
            }
        }

        if (resources == null) {
            resources = finder.findAll();
        }

        return ReferenceResolver.resolveRemaining(
            scope,
            arguments,
            filters,
            resources);
    }

    private Map<String, String> getFilters(Scope scope, List<Filter> filters) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Map<String, String> finderFilters = new HashMap<>();

        for (Iterator<Filter> i = filters.iterator(); i.hasNext(); ) {
            Filter filter = i.next();

            if (filter instanceof AndFilter) {
                List<Filter> childFilters = new ArrayList<>(((AndFilter) filter).getChildren());
                Map<String, String> childFinderFilters = getFilters(scope, childFilters);

                if (childFilters.isEmpty() && !childFilters.isEmpty()) {
                    finderFilters.putAll(childFinderFilters);
                    i.remove();
                }

            } else if (filter instanceof ComparisonFilter) {
                ComparisonFilter comparison = (ComparisonFilter) filter;

                if ("=".equals(comparison.getOperator())) {
                    finderFilters.put(comparison.getKey(), (String) evaluator.visit(comparison.getValue(), scope));
                    i.remove();
                }
            }
        }

        return finderFilters;
    }

}
