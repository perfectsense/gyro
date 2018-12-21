package beam.lang;

import beam.lang.types.ResourceBlock;

public abstract class BeamLanguageExtension extends ResourceBlock {

    private BeamInterp interp;

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

    public BeamInterp getInterp() {
        return interp;
    }

    public void setInterp(BeamInterp interp) {
        this.interp = interp;
    }

}
