package beam.lang.types;

import org.apache.commons.lang.StringUtils;

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
        StringBuilder sb = new StringBuilder();

        sb.append("[\n");

        List<String> out = new ArrayList<>();
        for (BeamValue value : getValues()) {
            out.add("    " + value.toString());
        }

        sb.append(StringUtils.join(out, ",\n"));
        sb.append("\n]\n");

        return sb.toString();
    }
}
