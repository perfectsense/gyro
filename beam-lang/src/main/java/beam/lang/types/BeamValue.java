package beam.lang.types;

import java.util.HashSet;
import java.util.Set;

public abstract class BeamValue<T> extends BeamReferable {

    private Set<BeamBlock> dependencies;

    public Set<BeamBlock> dependencies() {
        if (dependencies == null) {
            dependencies = new HashSet<>();
        }

        return dependencies;
    }

    public abstract T getValue();

}
