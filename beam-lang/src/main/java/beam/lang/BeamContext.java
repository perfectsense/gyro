package beam.lang;

import java.util.List;

public interface BeamContext {

    boolean containsKey(BeamContextKey key);

    BeamReferable get(BeamContextKey key);

    void add(BeamContextKey key, BeamReferable value);

    BeamReferable remove(BeamContextKey key);

    List<BeamContextKey> keys();

    List<BeamContextKey> getScope();

}
