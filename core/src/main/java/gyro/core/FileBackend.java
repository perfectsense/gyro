package gyro.core;

import gyro.core.scope.RootScope;

public abstract class FileBackend {

    public abstract String name();

    public abstract boolean load(RootScope scope) throws Exception;

    public abstract void save(RootScope scope) throws Exception;

    public abstract void delete(String path);

}
