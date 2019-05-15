package gyro.core;

import java.util.Set;

import gyro.core.resource.Resource;

public abstract class Credentials<T> extends Resource {

    /**
     * Return the name of this cloud.
     *
     * @return Never {@code null}.
     */
    public abstract String getCloudName();

    public T findCredentials() {
        return findCredentials(false);
    }

    public abstract T findCredentials(boolean refresh);

    public T findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
    }

    @Override
    public final boolean refresh() {
        return false;
    }

    @Override
    public final void create() {

    }

    @Override
    public final void update(Resource current, Set<String> changedFieldNames) {

    }

    @Override
    public final void delete() {

    }

    @Override
    public final String toDisplayString() {
        return null;
    }

}
