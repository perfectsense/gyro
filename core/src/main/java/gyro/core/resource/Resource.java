package gyro.core.resource;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gyro.core.auth.Credentials;

public abstract class Resource extends Diffable {

    public abstract boolean refresh();

    public abstract void create();

    public void afterCreate() {
    }

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

    public abstract void update(Resource current, Set<String> changedFieldNames);

    public void afterUpdate() {
    }

    public abstract void delete();

    public void afterDelete() {
    }

    @SuppressWarnings("unchecked")
    public <C extends Credentials> C credentials(Class<C> credentialsClass) {
        return Credentials.getInstance(credentialsClass, getClass(), scope);
    }

    public Object get(String key) {
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getField(key))
                .map(f -> f.getValue(this))
                .orElse(null);
    }

}
