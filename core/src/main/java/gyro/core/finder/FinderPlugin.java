package gyro.core.finder;

import gyro.core.plugin.Plugin;
import gyro.core.resource.RootScope;

public class FinderPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Finder.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Finder> finderClass = (Class<? extends Finder>) aClass;

            root.getSettings(FinderSettings.class).getFinderClasses().put(
                FinderType.getInstance(finderClass).getName(),
                finderClass);
        }
    }

}
