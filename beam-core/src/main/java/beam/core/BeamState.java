package beam.core;

import beam.lang.BeamFile;

public abstract class BeamState {

    public abstract String name();

    public abstract BeamFile load(BeamFile fileNode, BeamCore core) throws Exception;

    public abstract void save(BeamFile state);

    public abstract void delete(String path);

}
