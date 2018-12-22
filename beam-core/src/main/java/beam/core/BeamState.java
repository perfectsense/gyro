package beam.core;

import beam.lang.nodes.ContainerNode;
import beam.lang.nodes.ResourceNode;

public abstract class BeamState extends ResourceNode {

    public abstract ContainerNode load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, ContainerNode state);

    public abstract void delete(String name);

    @Override
    public String getResourceType() {
        return "state";
    }

}
