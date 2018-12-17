package beam.lang.types;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BeamMap extends BeamValue<Map> {

    private List<KeyValueBlock> keyValues;

    public List<KeyValueBlock> getKeyValues() {
        if (keyValues == null) {
            keyValues = new ArrayList<>();
        }

        return keyValues;
    }

    @Override
    public Map getValue() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");

        List<String> out = new ArrayList<>();
        for (KeyValueBlock block : getKeyValues()) {
            out.add("    " + block.toString());
        }

        sb.append(StringUtils.join(out, ",\n"));
        sb.append("\n}\n");

        return sb.toString();
    }

}
