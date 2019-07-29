package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.Type;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

import java.util.Optional;

public class FileBackendPlugin extends Plugin {
    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {

        if (FileBackend.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends FileBackend> fileBackendClass = (Class<? extends FileBackend>) aClass;


            String type = Optional.ofNullable(fileBackendClass.getAnnotation(Type.class))
                    .map(Type::value)
                    .orElse(null);

                root.getSettings(FileBackendSettings.class)
                        .getFileBackendClasses()
                        .put(type, fileBackendClass);

        }

    }
}
