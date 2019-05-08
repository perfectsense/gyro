package gyro.core.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import gyro.core.Credentials;
import gyro.core.GyroException;
import gyro.lang.query.AndQuery;
import gyro.lang.query.ComparisonQuery;
import gyro.lang.query.OrQuery;
import gyro.lang.query.Query;
import gyro.lang.query.QueryVisitor;

public class QueryEvaluator implements QueryVisitor<QueryContext, List<Resource>> {

    private final NodeEvaluator nodeEvaluator;

    public QueryEvaluator(NodeEvaluator nodeEvaluator) {
        this.nodeEvaluator = nodeEvaluator;
    }

    public Query optimize(Query query, ResourceFinder<Resource> finder, Scope scope) {
        if (query instanceof AndQuery) {
            Map<String, String> filters = new HashMap<>();
            List<Query> unsupported = new ArrayList<>();
            List<Query> newChildren = new ArrayList<>();

            for (Query child : ((AndQuery) query).getChildren()) {
                if (child instanceof ComparisonQuery) {
                    ComparisonQuery comparisonQuery = (ComparisonQuery) child;

                    if (isSupported(comparisonQuery, finder)) {
                        filters.putAll(getFilter(comparisonQuery, scope));

                    } else {
                        unsupported.add(child);
                    }

                } else {
                    unsupported.add(child);
                }
            }

            if (!filters.isEmpty()) {
                newChildren.add(new FoundQuery(finder.find(findQueryCredentials(scope), filters)));
            }

            newChildren.addAll(unsupported);

            return new AndQuery(newChildren);

        } else if (query instanceof ComparisonQuery) {
            ComparisonQuery comparisonQuery = (ComparisonQuery) query;

            if (isSupported(comparisonQuery, finder)) {
                return new FoundQuery(finder.find(findQueryCredentials(scope), getFilter(comparisonQuery, scope)));

            } else {
                return query;
            }

        } else if (query instanceof OrQuery) {
            List<Query> newChildren = new ArrayList<>();

            for (Query child : ((OrQuery) query).getChildren()) {
                newChildren.add(optimize(child, finder, scope));
            }

            return new OrQuery(newChildren);

        } else {
            throw new IllegalStateException();
        }
    }

    private boolean isSupported(ComparisonQuery comparisonQuery, ResourceFinder finder) {
        String path = comparisonQuery.getPath();
        String operator = comparisonQuery.getOperator();
        String mapFieldName = path.split("\\.")[0];

        for (ResourceFinderField field : ResourceFinderType.getInstance(finder.getClass()).getFields()) {
            String key = field.getGyroName();

            if (path.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)
                || path.contains(".") && mapFieldName.equals(key) && operator.equals(ComparisonQuery.EQUALS_OPERATOR)) {

                return true;
            }
        }

        return false;
    }

    private Map<String, String> getFilter(ComparisonQuery comparisonQuery, Scope scope) {
        Object comparisonValue = nodeEvaluator.visit(comparisonQuery.getValue(), scope);
        String path = comparisonQuery.getPath();
        Map<String, String> filter = new HashMap<>();

        if (comparisonValue != null) {
            if (path.contains(".")) {
                String mapFieldName = path.split("\\.")[0];
                String mapKey = path.replaceFirst(mapFieldName + ".", "");
                filter.put(String.format("%s:%s", mapFieldName, mapKey), comparisonValue.toString());

            } else {
                filter.put(path, comparisonValue.toString());
            }
        }

        return filter;
    }


    public Credentials findQueryCredentials(Scope scope) {
        scope = scope.getRootScope();

        if (scope != null) {
            String name = (String) scope.get("resource-credentials");

            if (name == null) {
                name = "default";
            }

            for (Resource resource : scope.getRootScope().findResources()) {
                if (resource instanceof Credentials) {
                    Credentials credentials = (Credentials) resource;

                    if (credentials.name().equals(name)) {
                        return credentials;
                    }
                }
            }
        }

        throw new IllegalStateException();
    }

    @Override
    public List<Resource> visitAnd(AndQuery query, QueryContext context) {
        List<Resource> resources = context.getResources();

        for (Query child : query.getChildren()) {
            resources = visit(child, context);
        }

        return resources;
    }

    @Override
    public List<Resource> visitComparison(ComparisonQuery query, QueryContext context) {
        List<Resource> resources = context.getResources();
        String path = query.getPath();

        for (Resource resource : resources) {
            Class<? extends Resource> resourceClass = resource.getClass();
            boolean validQuery = false;

            for (DiffableField field : DiffableType.getInstance(resourceClass).getFields()) {
                String key = field.getName();

                if (key.equals(path)) {
                    validQuery = true;
                }
            }

            if (!validQuery) {
                throw new GyroException(String.format(
                    "No such field [%s] defined %s!",
                    path, resourceClass));
            }
        }

        String operator = query.getOperator();
        Object comparisonValue = nodeEvaluator.visit(query.getValue(), context.getScope());

        if (ComparisonQuery.EQUALS_OPERATOR.equals(operator)) {
            return resources.stream()
                .filter(r -> Objects.equals(
                    DiffableType.getInstance(r.getClass()).getField(path).getValue(r), comparisonValue))
                .collect(Collectors.toList());

        } else if (ComparisonQuery.NOT_EQUALS_OPERATOR.equals(operator)) {
            return resources.stream()
                .filter(r -> !Objects.equals(
                    DiffableType.getInstance(r.getClass()).getField(path).getValue(r), comparisonValue))
                .collect(Collectors.toList());

        } else {
            throw new UnsupportedOperationException(String.format("Operator %s is not supported!", operator));
        }
    }

    @Override
    public List<Resource> visitOr(OrQuery query, QueryContext context) {
        List<Resource> joined = new ArrayList<>();

        for (Query child : query.getChildren()) {
            joined.addAll(visit(child, context));
        }

        return joined;
    }

}
