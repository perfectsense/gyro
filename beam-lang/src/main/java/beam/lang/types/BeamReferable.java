package beam.lang.types;

import java.util.Set;

public interface BeamReferable {

    boolean resolve(ContainerBlock context);

    Object getValue();

    Set<BeamReference> getDependencies(BeamBlock config);

}
