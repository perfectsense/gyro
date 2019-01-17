package beam.lang.plugins;

import beam.core.BeamCore;

public abstract class Plugin {

    private String artifact;
    private BeamCore core;

    public abstract String name();

    public void init() {

    }

    public void classLoaded(Class<?> klass) {

    }

    public final void artifact(String artifact) {
        this.artifact = artifact;
    }

    public final String artifact() {
        return artifact;
    }

    public final BeamCore core() {
        return core;
    }

    public final void core(BeamCore core) {
        this.core = core;
    }

}
