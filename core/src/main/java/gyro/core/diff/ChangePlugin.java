package gyro.core.diff;

import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class ChangePlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {
        if (ChangeProcessor.class.isAssignableFrom(aClass)) {
            ChangeProcessor processor = (ChangeProcessor) Reflections.newInstance(aClass);

            root.getSettings(ChangeSettings.class)
                .getProcessors()
                .add(processor);
        }
    }

}
