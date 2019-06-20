package gyro.core.reference;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import gyro.core.finder.QueryContext;
import gyro.core.finder.QueryEvaluator;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.query.Query;

public abstract class ReferenceResolver {

    public static Object resolveRemaining(Scope scope, List<Object> arguments, List<Query> queries, Object value) {
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

        if (queries == null || queries.isEmpty()) {
            return value;
        }

        QueryEvaluator evaluator = new QueryEvaluator();

        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                .filter(v -> queries.stream().allMatch(q -> evaluator.visit(q, new QueryContext(scope, v))))
                .collect(Collectors.toList());

        } else {
            for (Query q : queries) {
                if (!evaluator.visit(q, new QueryContext(scope, value))) {
                    return null;
                }
            }

            return value;
        }
    }

    public abstract String getName();

    public abstract Object resolve(Scope scope, List<Object> arguments, List<Query> queries) throws Exception;

}
