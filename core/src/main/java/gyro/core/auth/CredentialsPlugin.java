package gyro.core.auth;

import java.util.Optional;

import gyro.core.NamespaceUtils;
import gyro.core.plugin.Plugin;
import gyro.core.resource.RootScope;

public class CredentialsPlugin implements Plugin {

    @Override
    public void onClassLoaded(RootScope rootScope, Class<?> loadedClass) {
        if (Credentials.class.isAssignableFrom(loadedClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Credentials<?>> credentialsClass = (Class<? extends Credentials<?>>) loadedClass;
            String namespacePrefix = NamespaceUtils.getNamespacePrefix(credentialsClass);

            String type = Optional.ofNullable(credentialsClass.getAnnotation(CredentialsType.class))
                .map(CredentialsType::value)
                .orElse("credentials");

            rootScope.getSettings(CredentialsSettings.class)
                .getCredentialsClasses()
                .put(namespacePrefix + type, credentialsClass);
        }
    }

}
