package gyro.core.auth;

import java.nio.file.Paths;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.NamespaceUtils;
import gyro.core.resource.DiffableScope;
import gyro.core.resource.FileScope;
import gyro.core.resource.Scope;

public abstract class Credentials {

    Scope scope;

    @SuppressWarnings("unchecked")
    public static <C extends Credentials> C getInstance(Class<C> credentialsClass, Class<?> contextClass, Scope scope) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        String name = diffableScope != null
            ? diffableScope.getSettings(CredentialsSettings.class).getUseCredentials()
            : null;

        name = NamespaceUtils.getNamespacePrefix(contextClass) + (name != null ? name : "default");

        Credentials credentials = scope.getRootScope()
            .getSettings(CredentialsSettings.class)
            .getCredentialsByName()
            .get(name);

        if (credentials == null) {
            throw new GyroException(String.format(
                "Can't find [%s] credentials!",
                name));
        }

        if (!credentialsClass.isInstance(credentials)) {
            throw new GyroException(String.format(
                "Need [%s] credentials but have [%s] instead!",
                credentialsClass.getName(),
                credentials.getClass().getName()));
        }

        return (C) credentials;
    }

    public Set<String> getNamespaces() {
        return ImmutableSet.of(NamespaceUtils.getNamespace(getClass()));
    }

    public void refresh() {
    }

    public GyroInputStream openInput(String file) {
        FileScope fileScope = scope.getFileScope();

        return fileScope.getRootScope()
            .openInput(Paths.get(fileScope.getFile())
                .getParent()
                .resolve(file)
                .toString());
    }

}
