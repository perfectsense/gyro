package beam.lang;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BeamList extends BeamValue implements BeamCollection {

    private List<BeamScalar> list;

    private List<Object> value;

    public List<BeamScalar> getList() {
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public void setList(List<BeamScalar> list) {
        this.list = list;
    }

    @Override
    public boolean resolve(BeamContext context) {
        boolean progress = false;
        List<Object> result = new ArrayList<>();
        for (BeamScalar beamScalar : getList()) {
            progress = beamScalar.resolve(context) || progress;
            if (beamScalar.getValue() != null) {
                result.add(beamScalar.getValue());
            } else {
                return false;
            }
        }

        value = result;
        return progress;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamConfig config) {
        Set<BeamReference> dependencies = new HashSet<>();
        if (getValue() != null) {
            return dependencies;
        }

        for (BeamResolvable referable : getList()) {
            dependencies.addAll(referable.getDependencies(config));
        }

        return dependencies;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public BeamReferable get(String reference) {
        if (value == null) {
            throw new IllegalStateException();
        }

        if (reference.matches("\\d+")) {
            // index out of range
            return getList().get(Integer.parseInt(reference));
        } else {
            throw new BeamLangException(String.format("Expecting numeric, getting %s", reference));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BeamScalar beamScalar : getList()) {
            sb.append(BeamInterp.ui().dump("- "));
            sb.append(beamScalar);
            sb.append("\n");
        }

        return sb.toString();
    }
}
