package gyro.core.reference;

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class ReferencePlugin extends Plugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (ReferenceResolver.class.isAssignableFrom(aClass)) {
            root.getSettings(ReferenceSettings.class)
                .addResolver((Class<? extends ReferenceResolver>) aClass);
        }
    }

}
