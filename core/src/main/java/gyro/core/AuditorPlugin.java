package gyro.core;

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class AuditorPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if(GyroAuditor.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<?> gyroAuditorClass = (Class<?>) aClass;
            String namespace = Reflections.getNamespace(gyroAuditorClass);
            String type = Reflections.getType(gyroAuditorClass);

            root.getSettings(AuditorSettings.class)
                    .getAuditorClasses()
                    .put(namespace + "::" + type, gyroAuditorClass);
        }
    }
}
