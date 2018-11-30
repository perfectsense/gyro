package beam.lang;

import java.util.Set;

public interface BeamResolvable {

    boolean resolve(BeamConfig config);

    Object getValue();

    Set<BeamReference> getDependencies(BeamConfig config);
}
