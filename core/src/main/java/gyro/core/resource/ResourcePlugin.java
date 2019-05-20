package gyro.core.resource;

import gyro.core.plugin.Plugin;

public class ResourcePlugin implements Plugin {

    @Override
    public void onClassLoaded(RootScope rootScope, Class<?> loadedClass) {
        if (Resource.class.isAssignableFrom(loadedClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Resource> resourceClass = (Class<? extends Resource>) loadedClass;

            rootScope.getResourceClasses().put(
                DiffableType.getInstance(resourceClass).getName(),
                resourceClass);
        }
    }

}
