package gyro.lang;

import gyro.core.query.QueryField;
import gyro.core.query.QueryType;
import gyro.lang.ast.scope.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExternalResourceQuery<R extends Resource> extends ResourceQuery<R> {

    private Credentials credentials;

    public abstract List<R> query(Map<String, String> filter);

    public abstract List<R> queryAll();

    @Override
    public boolean external() {
        return true;
    }

    public Credentials credentials() {
        return credentials;
    }

    public void credentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public final List<R> query() {
        Map<String, String> filters = new HashMap<>();
        for (QueryField field : QueryType.getInstance(getClass()).getFields()) {
            Object value = field.getValue(this);
            String filterName = field.getFilterName();
            if (value != null) {
                filters.put(filterName, value.toString());
            }
        }

        return filters.isEmpty() ? queryAll() : query(filters);
    }

    @Override
    public final List<R> filter(List<R> resources) {
        throw new IllegalStateException();
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
