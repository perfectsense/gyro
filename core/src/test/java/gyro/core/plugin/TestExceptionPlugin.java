package gyro.core.plugin;

import gyro.core.scope.RootScope;

public class TestExceptionPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {
        throw new Exception();
    }

}
