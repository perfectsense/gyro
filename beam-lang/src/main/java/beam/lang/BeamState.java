package beam.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BeamState implements BeamContext {

    private Map<BeamContextKey, BeamReferable> context = new HashMap<>();

    @Override
    public BeamReferable get(BeamContextKey key) {
        return context.get(key);
    }

    @Override
    public boolean containsKey(BeamContextKey key) {
        return context.containsKey(key);
    }

    @Override
    public void add(BeamContextKey key, BeamReferable value) {
        context.put(key, value);
    }

    @Override
    public BeamReferable remove(BeamContextKey key) {
        return context.remove(key);
    }

    @Override
    public Set<BeamContextKey> keys() {
        return context.keySet();
    }

}
