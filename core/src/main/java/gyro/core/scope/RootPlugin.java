package gyro.core.scope;

import gyro.core.Reflections;
import gyro.core.plugin.Plugin;

public class RootPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (RootProcessor.class.isAssignableFrom(aClass)) {
            RootProcessor processor = (RootProcessor) Reflections.newInstance(aClass);

            root.getSettings(RootSettings.class)
                .getProcessors()
                .add(processor);
        }
    }

}
