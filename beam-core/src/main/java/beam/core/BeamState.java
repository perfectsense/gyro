package beam.core;

import beam.lang.FileNode;
import beam.lang.ResourceNode;

public abstract class BeamState extends ResourceNode {

    public abstract FileNode load(FileNode fileNode, BeamCore core) throws Exception;

    public abstract void save(FileNode state);

    public abstract void delete(String path);

    @Override
    public String resourceType() {
        return "state";
    }

}
