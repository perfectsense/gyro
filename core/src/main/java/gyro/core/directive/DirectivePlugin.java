package gyro.core.directive;

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;

public class DirectivePlugin extends Plugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (DirectiveProcessor.class.isAssignableFrom(aClass)) {
            root.getSettings(DirectiveSettings.class)
                .addProcessor((Class<? extends DirectiveProcessor<? extends Scope>>) aClass);
        }
    }

}
