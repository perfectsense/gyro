package beam.core;

import beam.lang.BeamLanguageExtension;
import beam.lang.types.BeamBlock;

public abstract class BeamState extends BeamLanguageExtension {

    public abstract BeamBlock load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, BeamBlock state);

    public abstract void delete(String name);

    @Override
    public String getResourceType() {
        return "state";
    }

}
