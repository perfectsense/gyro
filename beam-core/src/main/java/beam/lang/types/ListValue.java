package beam.lang.types;

import beam.lang.BeamLanguageException;
import beam.lang.Node;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ListValue extends Value<List> {

    private List<Value> values;

    public List<Value> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }

        return values;
    }

    @Override
    public void parentNode(Node parentNode) {
        super.parentNode(parentNode);

        for (Value value : getValues()) {
            value.parentNode(parentNode);
        }
    }

    @Override
    public List getValue() {
        List<String> list = new ArrayList();
        for (Value value : getValues()) {
            Object item = value.getValue();
            if (item != null) {
                list.add(item.toString());
            } else {
                list.add(value.toString());
            }
        }

        return list;
    }

    @Override
    public ListValue copy() {
        ListValue listNode = new ListValue();

        for (Value value : getValues()) {
            listNode.getValues().add(value.copy());
        }

        return listNode;
    }

    @Override
    public boolean resolve() {
        for (Value value : getValues()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unabled to resolve configuration.", value);
            }
        }

        return true;
    }

    @Override
    public String serialize(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append("[\n");

        List<String> out = new ArrayList<>();
        for (Value value : getValues()) {
            out.add(StringUtils.repeat(" ", indent) + value.toString());
        }

        sb.append(StringUtils.join(out, ",\n"));
        sb.append("\n]\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return serialize(0);
    }

}
