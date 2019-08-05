package gyro.core;

import gyro.core.backend.FileBackendSettings;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Stream;

public abstract class FileBackend {

    RootScope rootScope;

    private String name;

    private String fileLocation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract Stream<String> list() throws Exception;

    public abstract InputStream openInput(String file) throws Exception;

    public abstract OutputStream openOutput(String file) throws Exception;

    public abstract void delete(String file) throws Exception;

    public abstract Set<String> getNameSpaces() throws Exception;

    public RootScope getRootScope() {
        return rootScope;
    }

    public void setRootScope(RootScope rootScope) {
        this.rootScope = rootScope;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    @SuppressWarnings("unchecked")
    public static <C extends FileBackend> C getInstance(Class<C> fileBackendClass, Class<?> contextClass, Scope scope) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        String name = diffableScope != null
                ? diffableScope.getSettings(FileBackendSettings.class).getFileBackendCredentials()
                : null;

        name = NamespaceUtils.getNamespacePrefix(contextClass) + (name != null ? name : "default");

        FileBackend fileBackend = scope.getRootScope()
                .getSettings(FileBackendSettings.class)
                .getFileBackendByName()
                .get(name);

        if (fileBackend == null) {
            throw new GyroException(String.format(
                    "Can't find @|bold %s|@ file settings!",
                    name));
        }

        if (!fileBackendClass.isInstance(fileBackend)) {
            throw new GyroException(String.format(
                    "Can't use @|bold %s|@ credentials because it's an instance of @|bold %s|@, not @|bold %s|@!",
                    name,
                    fileBackend.getClass().getName(),
                    fileBackendClass.getName()));
        }

        return (C) fileBackend;
    }
}
