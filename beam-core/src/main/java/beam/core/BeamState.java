package beam.core;

import beam.lang.FileNode;

public abstract class BeamState {

    public abstract String name();

    public abstract FileNode load(FileNode fileNode, BeamCore core) throws Exception;

    public abstract void save(FileNode state);

    public abstract void delete(String path);

}
