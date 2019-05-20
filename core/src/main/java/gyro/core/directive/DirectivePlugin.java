package gyro.core.directive;

import gyro.core.plugin.Plugin;
import gyro.core.resource.RootScope;

public class DirectivePlugin implements Plugin {

    @Override
    public void onClassLoaded(RootScope rootScope, Class<?> loadedClass) throws IllegalAccessException, InstantiationException {
        if (DirectiveProcessor.class.isAssignableFrom(loadedClass)) {
            DirectiveProcessor processor = (DirectiveProcessor) loadedClass.newInstance();

            rootScope.getSettings(DirectiveSettings.class)
                .getProcessors()
                .put(processor.getName(), processor);
        }
    }

}
