package beam.core;

import java.util.HashMap;
import java.util.Map;

public class BeamMap implements BeamReferable {

    private Map<String, BeamReferable> map;

    private Map value;

    private boolean resolved;

    public Map<String, BeamReferable> getMap() {
        if (map == null) {
            map = new HashMap<>();
        }

        return map;
    }

    public void setMap(Map<String, BeamReferable> map) {
        this.map = map;
    }

    @Override
    public boolean resolve(BeamContext context) {
        if (resolved) {
            return false;
        }

        Map result = new HashMap();
        for (String key : getMap().keySet()) {
            BeamReferable referable = getMap().get(key);
            referable.resolve(context);
            if (referable.getValue() != null) {
                result.put(key, referable.getValue());
            } else {
                return false;
            }
        }

        value = result;
        resolved = true;
        return true;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
