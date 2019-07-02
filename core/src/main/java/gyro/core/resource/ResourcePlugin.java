package gyro.core.resource;

import gyro.core.plugin.Plugin;

public class ResourcePlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Resource.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Resource> resourceClass = (Class<? extends Resource>) aClass;

            root.put(
                DiffableType.getInstance(resourceClass).getName(),
                resourceClass);
        }
    }

}
