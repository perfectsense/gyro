package gyro.core.auth;

import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class CredentialsPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Credentials.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Credentials> credentialsClass = (Class<? extends Credentials>) aClass;
            String namespace = Reflections.getNamespace(credentialsClass);
            String type = Reflections.getTypeOptional(credentialsClass).orElse("credentials");

            root.getSettings(CredentialsSettings.class)
                .getCredentialsClasses()
                .put(namespace + "::" + type, credentialsClass);
        }
    }

}
