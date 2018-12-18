package beam.lang.types;

import beam.lang.BeamLanguageException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BeamList extends BeamValue<List> {

    private List<BeamValue> values;

    public List<BeamValue> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }

        return values;
    }

    @Override
    public List getValue() {
        List<String> list = new ArrayList();
        for (BeamValue value : getValues()) {
            Object item = value.getValue();
            if (item != null) {
                list.add(item.toString());
            }
        }

        return list;
    }

    @Override
    public boolean resolve() {
        for (BeamValue value : getValues()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unabled to resolve configuration.", value);
            }
        }

        return true;
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
