package beam.lang;

import beam.lang.ast.FileScope;

public abstract class StateBackend {

    public abstract String name();

    public abstract FileScope load(FileScope scope) throws Exception;

    public abstract void save(BeamFile state);

    public abstract void save(FileScope state);

    public abstract void delete(String path);

}
