package beam.lang;

import beam.core.BeamException;

import java.util.ArrayList;
import java.util.List;

public class BeamList implements BeamResolvable, BeamCollection {

    private List<BeamResolvable> list;

    private List value;

    public List<BeamResolvable> getList() {
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public void setList(List<BeamResolvable> list) {
        this.list = list;
    }

    @Override
    public boolean resolve(BeamConfig config) {
        if (value != null) {
            return false;
        }

        List result = new ArrayList();
        for (BeamResolvable referable : getList()) {
            referable.resolve(config);
            if (referable.getValue() != null) {
                result.add(referable.getValue());
            } else {
                return false;
            }
        }

        value = result;
        return true;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public BeamResolvable get(String reference) {
        if (value == null) {
            throw new IllegalStateException();
        }

        if (reference.matches("\\d+")) {
            // index out of range
            return getList().get(Integer.parseInt(reference));
        } else {
            throw new BeamException(String.format("Expecting numeric, getting %s", reference));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BeamResolvable resolvable : getList()) {
            sb.append(BCL.ui().dump("- "));
            sb.append(resolvable);
            sb.append("\n");
        }

        return sb.toString();
    }
}
