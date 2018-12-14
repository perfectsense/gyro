package beam.lang;

import java.util.Set;

public interface BeamContext {

    boolean containsKey(BeamContextKey key);

    BeamReferable get(BeamContextKey key);

    void add(BeamContextKey key, BeamReferable value);

    BeamReferable remove(BeamContextKey key);

    Set<BeamContextKey> keys();

}
