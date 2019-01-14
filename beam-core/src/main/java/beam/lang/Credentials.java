package beam.lang;

import beam.lang.Resource;

import java.util.Map;
import java.util.Set;

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
    public final void execute() {

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

    @Override
    public final Class resourceCredentialsClass() {
        return null;
    }

}
