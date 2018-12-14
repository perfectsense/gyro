package beam.lang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BeamMap extends BeamValue implements BeamCollection {

    private Map<String, BeamValue> map;

    private Map<String, Object> value;

    public Map<String, BeamValue> getMap() {
        if (map == null) {
            map = new HashMap<>();
        }

        return map;
    }

    public void setMap(Map<String, BeamValue> map) {
        this.map = map;
    }

    @Override
    public boolean resolve(BeamContext context) {
        if (value != null) {
            return false;
        }

        Map<String, Object> result = new HashMap<>();
        for (String key : getMap().keySet()) {
            BeamValue beamValue = getMap().get(key);
            beamValue.resolve(context);
            if (beamValue.getValue() != null) {
                result.put(key, beamValue.getValue());
            } else {
                return false;
            }
        }

        value = result;
        return true;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
        Set<BeamReference> dependencies = new HashSet<>();
        if (getValue() != null) {
            return dependencies;
        }

        for (String key : getMap().keySet()) {
            BeamResolvable referable = getMap().get(key);
            dependencies.addAll(referable.getDependencies(config));
        }

        return dependencies;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public BeamReferable get(String key) {
        if (value == null) {
            throw new IllegalStateException();
        }

        return getMap().get(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key : getMap().keySet()) {
            sb.append(BeamInterp.ui().dump(key));
            sb.append(":");
            BeamValue beamValue = getMap().get(key);

            if (beamValue instanceof BeamCollection) {
                sb.append("\n");

                BeamInterp.ui().indent();
                sb.append(beamValue);
                BeamInterp.ui().unindent();

                sb.append(BeamInterp.ui().dump("end\n"));
            } else {
                sb.append(" ");
                sb.append(beamValue);
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
