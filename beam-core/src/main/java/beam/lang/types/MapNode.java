package beam.lang.types;

import beam.lang.BeamLanguageException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapNode extends ValueNode<Map> {

    private Map<String, ValueNode> keyValues;

    public Map<String, ValueNode> getKeyValues() {
        if (keyValues == null) {
            keyValues = new HashMap<>();
        }

        return keyValues;
    }

    public void put(String key, ValueNode valueNode) {
        valueNode.setParentBlock(this);

        getKeyValues().put(key, valueNode);
    }

    @Override
    public void setParentBlock(Node parentBlock) {
        super.setParentBlock(parentBlock);

        for (ValueNode valueNode : getKeyValues().values()) {
            valueNode.setParentBlock(parentBlock);
        }
    }

    @Override
    public Map getValue() {
        Map<String, Object> map = new HashMap<>();
        for (String key : getKeyValues().keySet()) {
            map.put(key, getKeyValues().get(key).getValue());
        }

        return map;
    }

    @Override
    public boolean resolve() {
        for (ValueNode valueNode : getKeyValues().values()) {
            boolean resolved = valueNode.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unabled to resolve configuration.", valueNode);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");

        List<String> out = new ArrayList<>();
        for (String key : getKeyValues().keySet()) {
            out.add(String.format("    %s: %s", key, getKeyValues().get(key).getValue()));
        }

        sb.append(StringUtils.join(out, ",\n"));
        sb.append("\n}\n");

        return sb.toString();
    }

}
