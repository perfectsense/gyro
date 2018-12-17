package beam.lang.types;

import java.util.ArrayList;
import java.util.List;

public class BeamMap extends BeamValue {

    private List<KeyValueBlock> keyValues;

    public List<KeyValueBlock> getKeyValues() {
        if (keyValues == null) {
            keyValues = new ArrayList<>();
        }

        return keyValues;
    }

    @Override
    public String stringValue() {
        return null;
    }

    @Override
    public String toString() {
        return "BeamMap{"
            + "keyValues="
            + keyValues + '}';
    }

}
