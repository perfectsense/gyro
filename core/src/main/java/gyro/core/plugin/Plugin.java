package gyro.core.plugin;

import gyro.core.resource.RootScope;

public interface Plugin {

    void onEachClass(RootScope root, Class<?> aClass) throws Exception;

}
