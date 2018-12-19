package beam.core;

import beam.lang.BeamLanguageExtension;
import beam.lang.types.ContainerBlock;

public abstract class BeamState extends BeamLanguageExtension {

    public abstract ContainerBlock load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, ContainerBlock state);

    public abstract void delete(String name);

    @Override
    public String getResourceType() {
        return "state";
    }

}
