package beam.lang;

import beam.core.BeamCore;
import beam.lang.nodes.ResourceNode;

public abstract class BeamLanguageExtension extends ResourceNode {

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
