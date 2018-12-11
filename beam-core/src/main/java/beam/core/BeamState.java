package beam.core;

import beam.lang.BeamConfig;

public abstract class BeamState extends BeamConfig {

    public abstract void load(String name, BeamCore core);

    public abstract void save(String name, BeamConfig state);

    public abstract void delete(String name);

    @Override
    public String getType() {
        return "state";
    }

}
