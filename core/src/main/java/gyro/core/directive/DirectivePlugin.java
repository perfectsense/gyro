package gyro.core.directive;

import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class DirectivePlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (DirectiveProcessor.class.isAssignableFrom(aClass)) {
            DirectiveProcessor processor = (DirectiveProcessor) Reflections.newInstance(aClass);

            root.getSettings(DirectiveSettings.class)
                .getProcessors()
                .put(processor.getName(), processor);
        }
    }

}
