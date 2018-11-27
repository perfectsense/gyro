package beam.lang;

import java.util.Set;

public interface BeamResolvable {

    boolean resolve(BeamConfig config);

    Object getValue();

    Set<BeamConfig> getDependencies(BeamConfig config);
}
