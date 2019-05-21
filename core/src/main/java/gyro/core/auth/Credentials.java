package gyro.core.auth;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.NamespaceUtils;
import gyro.core.resource.DiffableScope;
import gyro.core.resource.FileScope;
import gyro.core.resource.Scope;

public abstract class Credentials<T> {

    public static Credentials<?> getInstance(Class<?> aClass, Scope scope) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        String name = diffableScope != null
            ? diffableScope.getSettings(CredentialsSettings.class).getUseCredentials()
            : null;

        name = NamespaceUtils.getNamespacePrefix(aClass) + (name != null ? name : "default");

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

    Scope scope;

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

    public InputStream openInput(String file) {
        FileScope fileScope = scope.getFileScope();

        return fileScope.getRootScope()
            .getBackend()
            .openInput(Paths.get(fileScope.getFile())
                .getParent()
                .resolve(file)
                .toString());
    }

}
