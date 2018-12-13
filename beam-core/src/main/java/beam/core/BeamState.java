package beam.core;

import beam.lang.BeamConfig;

public abstract class BeamState extends BeamConfig {

    public abstract BeamConfig load(String name, BeamCore core) throws Exception;

    public abstract void save(String name, BeamConfig state);

    public abstract void delete(String name);

    @Override
    public String getType() {
        return "state";
    }

}
