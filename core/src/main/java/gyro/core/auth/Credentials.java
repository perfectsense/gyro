package gyro.core.auth;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.NamespaceUtils;

public abstract class Credentials<T> {

    public Set<String> getNamespaces() {
        return ImmutableSet.of(NamespaceUtils.getNamespace(getClass()));
    }

    public T findCredentials() {
        return findCredentials(false);
    }

    public abstract T findCredentials(boolean refresh);

    public T findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
    }

}
