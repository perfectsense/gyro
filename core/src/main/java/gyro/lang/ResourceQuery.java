package gyro.lang;

import gyro.core.BeamException;
import gyro.core.diff.DiffableType;
import gyro.core.query.QueryField;
import gyro.core.query.QueryType;
import gyro.lang.ast.query.ComparisonQuery;
import gyro.lang.ast.scope.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class ResourceQuery<T extends Resource> {

    private boolean apiQuery;

    private String operator;

    private Credentials credentials;

    public abstract List<T> query(Map<String, String> filter);

    public abstract List<T> queryAll();

    public boolean apiQuery() {
        return apiQuery;
    }

    public void apiQuery(boolean apiQuery) {
        this.apiQuery = apiQuery;
    }

    public String operator() {
        return operator;
    }

    public void operator(String operator) {
        this.operator = operator;
    }

    public Credentials credentials() {
        return credentials;
    }

    public void credentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public List<T> query() {
        if (apiQuery()) {
            Map<String, String> filters = new HashMap<>();
            for (QueryField field : QueryType.getInstance(getClass()).getFields()) {
                Object value = field.getValue(this);
                String filterName = field.getFilterName();
                if (value != null) {
                    filters.put(filterName, value.toString());
                }
            }

            return filters.isEmpty() ? queryAll() : query(filters);

        } else {
            throw new UnsupportedOperationException("Non api query is not supported, use `filter` instead!");
        }
    }

    public List<T> filter(List<T> resources) {
        if (resources == null || resources.isEmpty()) {
            resources = queryAll();
        }

        for (QueryField field : QueryType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();
            Object value = field.getValue(this);

            if (value != null) {
                if (ComparisonQuery.EQUALS_OPERATOR.equals(operator)) {
                    resources = resources.stream()
                        .filter(r -> Objects.equals(
                            DiffableType.getInstance(r.getClass()).getFieldByBeamName(key).getValue(r), value))
                        .collect(Collectors.toList());

                } else if (ComparisonQuery.NOT_EQUALS_OPERATOR.equals(operator)) {
                    resources = resources.stream()
                        .filter(r -> !Objects.equals(
                            DiffableType.getInstance(r.getClass()).getFieldByBeamName(key).getValue(r), value))
                        .collect(Collectors.toList());

                } else {
                    throw new UnsupportedOperationException(String.format("%s operator is not supported!", operator));
                }
            }
        }

        return resources;
    }

    public boolean merge(ResourceQuery<Resource> other) {
        if (apiQuery() && other.apiQuery()) {
            for (QueryField field : QueryType.getInstance(getClass()).getFields()) {
                String key = field.getBeamName();
                Object value = field.getValue(this);
                Object otherValue = field.getValue(other);
                if (value != null && otherValue != null) {
                    throw new BeamException(String.format("%s is filtered more than once", key));
                } else if (otherValue != null) {
                    field.setValue(this, otherValue);
                }
            }

            return true;
        }

        return false;
    }

    public Credentials resourceCredentials(Scope scope) {

        scope = scope.getRootScope();

        if (scope != null) {
            String name = (String) scope.get("resource-credentials");

            if (name == null) {
                name = "default";
            }

            for (Resource resource : scope.getRootScope().findAllResources()) {
                if (resource instanceof Credentials) {
                    Credentials credentials = (Credentials) resource;

                    if (credentials.resourceIdentifier().equals(name)) {
                        return credentials;
                    }
                }
            }
        }

        throw new IllegalStateException();
    }
}
