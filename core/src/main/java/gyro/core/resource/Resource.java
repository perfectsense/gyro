package gyro.core.resource;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gyro.core.Credentials;

public abstract class Resource extends Diffable {

    public String primaryKey() {
        return String.format("%s::%s", DiffableType.getInstance(getClass()).getName(), name());
    }

    public abstract boolean refresh();

    public abstract void create();

    public void testCreate() {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            if (field.getTestValue() != null) {
                String value = "test-" + field.getTestValue();

                if (field.isTestValueRandomSuffix())  {
                    value += "-";
                    value += UUID.randomUUID().toString().replaceAll("-", "").substring(16);
                }

                field.setValue(this, value);
            }
        }
    }

    public abstract void update(Resource current, Set<String> changedProperties);

    public abstract void delete();

    public Credentials resourceCredentials() {
        for (Resource r = this; r != null; r = r.parentResource()) {
            Scope scope = r.scope();

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
        }

        throw new IllegalStateException();
    }

    public Object get(String key) {
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getFieldByName(key))
                .map(f -> f.getValue(this))
                .orElse(null);
    }

}
