package gyro.lang;

import gyro.core.BeamException;
import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.lang.ast.scope.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ResourceQuery<T extends Resource> extends Diffable {

    public abstract List<T> query(Map<String, String> filter);

    public abstract List<T> queryAll();

    public final List<T> query() {
        Map<String, String> filters = new HashMap<>();
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();
            Object value = field.getValue(this);
            if (value != null) {
                // need to test if the resource query is an api resource query
                filters.put(key, value.toString());
            }
        }

        List<T> resources = query(filters);
        resources.stream().forEach(s -> System.out.println(s.toDisplayString()));
        return resources;
    }

    public void merge(ResourceQuery<Resource> other) {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();
            Object value = field.getValue(this);
            Object otherValue = field.getValue(other);
            if (value != null && otherValue != null) {
                throw new BeamException(String.format("%s is filtered more than once", key));
            } else if (otherValue != null) {
                field.setValue(this, otherValue);
            }
        }
    }

    @Override
    public String primaryKey() {
        return null;
    }

    @Override
    public String toDisplayString() {
        return null;
    }

    public Credentials resourceCredentials() {

            Scope scope = scope().getRootScope();

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
/*

step 1: extract or

A | b or c and D | e and G or H

[A] | [b] or [c] and [D] | [e] and [G] or [H]

[A] | [b] or [c, D] | [e, G] or [H]

[A, b] or [A, c, D] | [e, G] or [H]

[A, b, e, G] or [A, b, H] or [A, c, D, e, G] or [A, c, D, H]

step 2: Locate api filter fields

[A, G] [b, e] or [A, H] [b] or [A, D, G] [c, e] or [A, D, H] [c]


*/