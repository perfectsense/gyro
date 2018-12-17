package beam.lang.types;

import java.util.HashSet;
import java.util.Set;

public abstract class BeamBlock extends BeamReferable {

    private Set<BeamBlock> dependencies;

    public abstract boolean resolve();

    public Set<BeamBlock> dependencies() {
        if (dependencies == null) {
            dependencies = new HashSet<>();
        }

        return dependencies;
    }

}
