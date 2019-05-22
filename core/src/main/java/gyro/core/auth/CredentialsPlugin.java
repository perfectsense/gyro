package gyro.core.auth;

import java.util.Optional;

import gyro.core.NamespaceUtils;
import gyro.core.Type;
import gyro.core.plugin.Plugin;
import gyro.core.resource.RootScope;

public class CredentialsPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Credentials.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Credentials> credentialsClass = (Class<? extends Credentials>) aClass;
            String namespacePrefix = NamespaceUtils.getNamespacePrefix(credentialsClass);

            String type = Optional.ofNullable(credentialsClass.getAnnotation(Type.class))
                .map(Type::value)
                .orElse("credentials");

            root.getSettings(CredentialsSettings.class)
                .getCredentialsClasses()
                .put(namespacePrefix + type, credentialsClass);
        }
    }

}
