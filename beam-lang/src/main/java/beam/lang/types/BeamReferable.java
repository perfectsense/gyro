package beam.lang.types;

import java.util.Set;

public abstract class BeamReferable {

    private BeamBlock parentBlock;

    public BeamBlock getParentBlock() {
        return parentBlock;
    }

    public void setParentBlock(BeamBlock parentBlock) {
        this.parentBlock = parentBlock;
    }

    public abstract boolean resolve();

    public abstract Set<BeamBlock> dependencies();

}
