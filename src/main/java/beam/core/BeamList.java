package beam.core;

import java.util.ArrayList;
import java.util.List;

public class BeamList implements BeamReferable {

    private List<BeamReferable> list;

    private List value;

    private boolean resolved;

    public List<BeamReferable> getList() {
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public void setList(List<BeamReferable> list) {
        this.list = list;
    }

    @Override
    public boolean resolve(BeamContext context) {
        if (resolved) {
            return false;
        }

        List result = new ArrayList();
        for (BeamReferable referable : getList()) {
            referable.resolve(context);
            if (referable.getValue() != null) {
                result.add(referable.getValue());
            } else {
                return false;
            }
        }

        value = result;
        resolved = true;
        return true;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
