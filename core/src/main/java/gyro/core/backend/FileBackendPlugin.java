package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.NamespaceUtils;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;
import gyro.core.Type;
import gyro.util.Bug;

import java.util.Optional;

public class FileBackendPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {
        if (FileBackend.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends FileBackend> fileBackendClass = (Class<? extends FileBackend>) aClass;
            String namespacePrefix = NamespaceUtils.getNamespacePrefix(fileBackendClass);

            String type = Optional.ofNullable(fileBackendClass.getAnnotation(Type.class))
                    .map(Type::value)
                    .orElse(null);

            if (type != null) {
                root.getSettings(FileBackendsSettings.class)
                        .getFileBackendsClasses()
                        .put(namespacePrefix + type, fileBackendClass);
            } else {
                throw new Bug("Loading file backend plugin failed. File Backend implementation is missing @Type annotation.");
            }
        }
    }

}
