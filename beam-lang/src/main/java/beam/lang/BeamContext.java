package beam.lang;

import java.util.List;

public interface BeamContext {

    boolean hasKey(BeamContextKey key);

    BeamReferable getReferable(BeamContextKey key);

    void addReferable(BeamContextKey key, BeamReferable value);

    BeamReferable removeReferable(BeamContextKey key);

    List<BeamContextKey> listContextKeys();

    void importContext(BeamContext context);

    List<BeamContextKey> getScope();
}
