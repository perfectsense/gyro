package beam.lang.types;

import beam.lang.BeamLanguageException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, Object> map = new HashMap<>();
        for (KeyValueBlock keyValueBlock : getKeyValues()) {
            map.put(keyValueBlock.getKey(), keyValueBlock.getValue().getValue());
        }

        return map;
    }

    @Override
    public boolean resolve() {
        for (KeyValueBlock keyValueBlock : getKeyValues()) {
            boolean resolved = keyValueBlock.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unabled to resolve configuration.", keyValueBlock);
            }
        }

        return true;
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
