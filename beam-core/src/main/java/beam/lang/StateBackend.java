package beam.lang;

import beam.lang.ast.scope.FileScope;

public abstract class StateBackend {

    public abstract String name();

    public abstract FileScope load(FileScope parent, String name) throws Exception;

    public abstract void save(FileScope state);

    public abstract void delete(String path);

}
