package beam.core;

import beam.lang.BeamBlock;

public abstract class BeamState extends BeamBlock {

    public abstract BeamBlock load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, BeamBlock state);

    public abstract void delete(String name);

    @Override
    public String getType() {
        return "state";
    }

}
