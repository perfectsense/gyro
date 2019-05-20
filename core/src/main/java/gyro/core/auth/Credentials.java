package gyro.core.auth;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.NamespaceUtils;
import gyro.core.resource.Scope;

public abstract class Credentials<T> {

    public static Credentials<?> getInstance(Class<?> aClass, Scope scope) {
        String name = NamespaceUtils.getNamespacePrefix(aClass) + "default";

        Credentials<?> credentials = scope.getRootScope()
            .getSettings(CredentialsSettings.class)
            .getCredentialsByName()
            .get(name);

        if (credentials == null) {
            throw new GyroException(String.format(
                "Can't find [%s] credentials!",
                name));
        }

        return credentials;
    }

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
