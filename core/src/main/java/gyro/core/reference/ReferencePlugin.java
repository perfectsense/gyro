package gyro.core.reference;

import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class ReferencePlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (ReferenceResolver.class.isAssignableFrom(aClass)) {
            ReferenceResolver resolver = (ReferenceResolver) Reflections.newInstance(aClass);

            root.getSettings(ReferenceSettings.class)
                .getResolvers()
                .put(resolver.getName(), resolver);
        }
    }

}
