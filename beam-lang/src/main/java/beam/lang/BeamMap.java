package beam.lang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BeamMap implements BeamResolvable, BeamCollection {

    private Map<String, BeamResolvable> map;

    private Map value;

    public Map<String, BeamResolvable> getMap() {
        if (map == null) {
            map = new HashMap<>();
        }

        return map;
    }

    public void setMap(Map<String, BeamResolvable> map) {
        this.map = map;
    }

    @Override
    public boolean resolve(BeamConfig config) {
        if (value != null) {
            return false;
        }

        Map result = new HashMap();
        for (String key : getMap().keySet()) {
            BeamResolvable referable = getMap().get(key);
            referable.resolve(config);
            if (referable.getValue() != null) {
                result.put(key, referable.getValue());
            } else {
                return false;
            }
        }

        value = result;
        return true;
    }

    @Override
    public Set<BeamConfig> getDependencies(BeamConfig config) {
        Set<BeamConfig> dependencies = new HashSet<>();
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
    public BeamResolvable get(String key) {
        if (value == null) {
            throw new IllegalStateException();
        }

        return getMap().get(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key : getMap().keySet()) {
            sb.append(BCL.ui().dump(key));
            sb.append(":");
            BeamResolvable value = getMap().get(key);

            if (value instanceof BeamCollection) {
                sb.append("\n");

                BCL.ui().indent();
                sb.append(value);
                BCL.ui().unindent();

                sb.append(BCL.ui().dump("end\n"));
            } else {
                sb.append(" ");
                sb.append(value);
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
