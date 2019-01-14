package beam.lang;

import beam.core.BeamCore;

public abstract class StateBackend {

    public abstract String name();

    public abstract BeamFile load(BeamFile fileNode, BeamCore core) throws Exception;

    public abstract void save(BeamFile state);

    public abstract void delete(String path);

}
