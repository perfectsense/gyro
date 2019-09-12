package gyro.core.diff;

import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class GlobalChangePlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (GlobalChangeProcessor.class.isAssignableFrom(aClass)) {
            GlobalChangeProcessor processor = (GlobalChangeProcessor) Reflections.newInstance(aClass);

            root.getSettings(ChangeSettings.class)
                .getProcessors()
                .add(processor);
        }
    }

}
