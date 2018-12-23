package beam.core;

import beam.lang.nodes.ContainerNode;
import beam.lang.nodes.ResourceNode;
import beam.lang.nodes.RootNode;

public abstract class BeamState extends ResourceNode {

    public abstract RootNode load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, ContainerNode state);

    public abstract void delete(String name);

    @Override
    public String getResourceType() {
        return "state";
    }

}
