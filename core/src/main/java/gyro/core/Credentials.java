package gyro.core;

import java.util.Map;
import java.util.Set;

import gyro.core.resource.Resource;

public abstract class Credentials extends Resource {

    /**
     * Return the name of this cloud.
     *
     * @return Never {@code null}.
     */
    public abstract String getCloudName();

    public Map<String, String> findCredentials() {
        return findCredentials(false);
    }

    public abstract Map<String, String> findCredentials(boolean refresh);

    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
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
    public final void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public final void delete() {

    }

    @Override
    public final String toDisplayString() {
        return null;
    }

}
