package gyro.core.directive;

import gyro.core.plugin.Plugin;
import gyro.core.resource.RootScope;

public class DirectivePlugin implements Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws IllegalAccessException, InstantiationException {
        if (DirectiveProcessor.class.isAssignableFrom(aClass)) {
            DirectiveProcessor processor = (DirectiveProcessor) aClass.newInstance();

            root.getSettings(DirectiveSettings.class)
                .getProcessors()
                .put(processor.getName(), processor);
        }
    }

}
