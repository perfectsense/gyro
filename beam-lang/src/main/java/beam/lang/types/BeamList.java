package beam.lang.types;

import java.util.ArrayList;
import java.util.List;

public class BeamList extends BeamValue {

    private List<BeamValue> values;

    public List<BeamValue> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }

        return values;
    }

    @Override
    public String stringValue() {
        return null;
    }

    @Override
    public String toString() {
        return "BeamList{" +
            "values=" + values +
            '}';
    }
}
