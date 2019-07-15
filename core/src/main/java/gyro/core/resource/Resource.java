package gyro.core.resource;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gyro.core.GyroUI;
import gyro.core.auth.Credentials;
import gyro.core.scope.State;

public abstract class Resource extends Diffable {

    public abstract boolean refresh();

    public abstract void create(GyroUI ui, State state);

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

    public abstract void update(GyroUI ui, State state, Resource current, Set<String> changedFieldNames);

    public abstract void delete(GyroUI ui, State state);

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
