package beam.lang;

import java.util.HashMap;
import java.util.Map;

public class BeamConfig implements BeamResolvable, BeamCollection {

    private Map<BeamConfigKey, BeamResolvable> context;

    public Map<BeamConfigKey, BeamResolvable> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }

        return context;
    }

    public void setContext(Map<BeamConfigKey, BeamResolvable> context) {
        this.context = context;
    }

    @Override
    public boolean resolve(BeamConfig config) {
        boolean progress = false;
        for (BeamConfigKey key : getContext().keySet()) {
            BeamResolvable referable = getContext().get(key);
            progress = referable.resolve(config) || progress;
        }

        return progress;
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public BeamResolvable get(String key) {
        return getContext().get(new BeamConfigKey(null, key));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BeamConfigKey key : getContext().keySet()) {
            BeamResolvable referable = getContext().get(key);
            sb.append(BCL.ui().dump(key.toString()));
            if (!(referable instanceof BeamConfig)) {
                sb.append(":");
            }

            if (referable instanceof BeamCollection) {
                sb.append("\n");

                BCL.ui().indent();
                sb.append(referable);
                BCL.ui().unindent();

                sb.append(BCL.ui().dump("end\n"));
            } else {
                sb.append(" ");
                sb.append(referable);
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
