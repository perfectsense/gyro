package gyro.core.plugin;

import gyro.core.resource.RootScope;

public interface Plugin {

    void onClassLoaded(RootScope rootScope, Class<?> loadedClass) throws Exception;

}
