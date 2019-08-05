package gyro.core;

import gyro.core.backend.FileBackendSettings;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

public abstract class FileBackend {

    private RootScope rootScope;
    private String name;

    public abstract Stream<String> list() throws Exception;

    public abstract InputStream openInput(String file) throws Exception;

    public abstract OutputStream openOutput(String file) throws Exception;

    public abstract void delete(String file) throws Exception;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RootScope getRootScope() {
        return rootScope;
    }

    public void setRootScope(RootScope rootScope) {
        this.rootScope = rootScope;
    }

    @SuppressWarnings("unchecked")
    public static <C extends FileBackend> C getInstance(Diffable diffable) {
        DiffableScope diffableScope = DiffableInternals.getScope(diffable);

        FileBackendSettings settings = diffableScope.getSettings(FileBackendSettings.class);
        String fileBackendName = settings.getUseFileBackend();
        FileBackend fileBackend = settings.getFileBackendByName().get(fileBackendName);
        if (fileBackend == null) {
            settings = diffableScope.getRootScope().getSettings(FileBackendSettings.class);
            fileBackend = settings.getFileBackendByName().get(fileBackendName);
        }

        if (fileBackend == null) {
            fileBackend = diffableScope.getRootScope().getBackend();
        }

        fileBackend.setRootScope(diffableScope.getRootScope());

        return (C) fileBackend;
    }

}
