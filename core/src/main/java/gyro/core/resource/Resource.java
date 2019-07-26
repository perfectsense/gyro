package gyro.core.resource;

import java.util.Optional;
import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.auth.Credentials;
import gyro.core.diff.Context;

public abstract class Resource extends Diffable {

    public abstract boolean refresh();

    public abstract void create(GyroUI ui, Context context) throws Exception;

    public abstract void update(GyroUI ui, Context context, Resource current, Set<String> changedFieldNames) throws Exception;

    public abstract void delete(GyroUI ui, Context context) throws Exception;

    public void testCreate(GyroUI ui, Context context) throws Exception {
        DiffableType.getInstance(getClass()).getFields().forEach(f -> f.testUpdate(this));
    }

    public <C extends Credentials> C credentials(Class<C> credentialsClass) {
        return Credentials.getInstance(credentialsClass, getClass(), scope);
    }

    public Object get(String key) {
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getField(key))
                .map(f -> f.getValue(this))
                .orElse(null);
    }

}
