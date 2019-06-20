package gyro.core.reference;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import gyro.core.finder.FilterContext;
import gyro.core.finder.FilterEvaluator;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.filter.Filter;

public abstract class ReferenceResolver {

    public static Object resolveRemaining(Scope scope, List<Object> arguments, List<Filter> filters, Object value) {
        if (value == null) {
            return null;
        }

        if (arguments != null) {
            for (Object argument : arguments) {
                value = NodeEvaluator.getValue(value, (String) argument);

                if (value == null) {
                    return null;
                }
            }
        }

        if (filters == null || filters.isEmpty()) {
            return value;
        }

        FilterEvaluator evaluator = new FilterEvaluator();

        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                .filter(v -> filters.stream().allMatch(f -> evaluator.visit(f, new FilterContext(scope, v))))
                .collect(Collectors.toList());

        } else {
            for (Filter f : filters) {
                if (!evaluator.visit(f, new FilterContext(scope, value))) {
                    return null;
                }
            }

            return value;
        }
    }

    public abstract String getName();

    public abstract Object resolve(Scope scope, List<Object> arguments, List<Filter> filters) throws Exception;

}
