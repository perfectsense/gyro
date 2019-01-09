package beam.core;

import beam.lang.ResourceNode;
import beam.lang.RootNode;

public abstract class BeamState extends ResourceNode {

    public abstract RootNode load(RootNode rootNode, BeamCore core) throws Exception;

    public abstract void save(RootNode state);

    public abstract void delete(String path);

    @Override
    public String resourceType() {
        return "state";
    }

}
