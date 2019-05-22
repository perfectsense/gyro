package gyro.core.plugin;

import java.util.HashMap;
import java.util.Map;

import gyro.core.resource.RootScope;

public class TestPlugin extends Plugin {

    public final Map<Class<?>, Integer> counts = new HashMap<>();

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        counts.compute(aClass, (c, v) -> (v != null ? v : 0) + 1);
    }

}
