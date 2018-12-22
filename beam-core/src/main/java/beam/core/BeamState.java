package beam.core;

import beam.lang.BeamLanguageExtension;
import beam.lang.types.ContainerNode;

public abstract class BeamState extends BeamLanguageExtension {

    public abstract ContainerNode load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, ContainerNode state);

    public abstract void delete(String name);

    @Override
    public String getResourceType() {
        return "state";
    }

}
