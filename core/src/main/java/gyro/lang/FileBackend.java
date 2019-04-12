package gyro.lang;

import gyro.core.scope.FileScope;

public abstract class FileBackend {

    public abstract String name();

    public abstract boolean load(FileScope scope) throws Exception;

    public abstract void save(FileScope scope) throws Exception;

    public abstract void delete(String path);

}
