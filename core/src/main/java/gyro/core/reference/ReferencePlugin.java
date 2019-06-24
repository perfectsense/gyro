package gyro.core.reference;

import gyro.core.plugin.Plugin;
import gyro.core.resource.RootScope;

public class ReferencePlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {
        if (ReferenceResolver.class.isAssignableFrom(aClass)) {
            ReferenceResolver resolver = (ReferenceResolver) aClass.newInstance();

            root.getSettings(ReferenceSettings.class)
                .getResolvers()
                .put(resolver.getName(), resolver);
        }
    }

}
