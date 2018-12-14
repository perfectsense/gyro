package beam.lang;

import java.util.Set;

public interface BeamResolvable {

    boolean resolve(BeamContext context);

    Object getValue();

    Set<BeamReference> getDependencies(BeamBlock config);
}
