package beam.lang;

import beam.lang.ast.Scope;

public abstract class StateBackend {

    public abstract String name();

    public abstract BeamFile load(BeamFile fileNode) throws Exception;

    public abstract Scope load(Scope scope) throws Exception;

    public abstract void save(BeamFile state);

    public abstract void save(Scope state);

    public abstract void delete(String path);

}
