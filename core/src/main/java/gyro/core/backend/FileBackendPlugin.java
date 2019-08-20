package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.NamespaceUtils;
import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class FileBackendPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (FileBackend.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends FileBackend> fileBackendClass = (Class<? extends FileBackend>) aClass;
            String namespace = NamespaceUtils.getNamespace(fileBackendClass);
            String type = Reflections.getType(fileBackendClass);

            root.getSettings(FileBackendsSettings.class)
                .getFileBackendsClasses()
                .put(namespace + "::" + type, fileBackendClass);
        }
    }

}
