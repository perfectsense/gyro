package beam.lang;

import beam.core.BeamCore;
import beam.lang.types.ResourceBlock;

public abstract class BeamLanguageExtension extends ResourceBlock {

    private BeamCore core;

    /**
     * `execute()` is called during the parsing of the configuration. This
     * allows extensions to perform any necessary actions to load themselves.
     */
    public void execute() {

    }

    final void executeInternal() {
        syncInternalToProperties();
        execute();
    }

    public BeamCore getCore() {
        return core;
    }

    public void setCore(BeamCore core) {
        this.core = core;
    }

}
