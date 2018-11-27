package beam.lang;

import java.util.ArrayList;
import java.util.List;

public class ForConfig extends BeamConfig {

    private boolean expanded;

    @Override
    public String getType() {
        return "for";
    }

    @Override
    protected boolean resolve(BeamConfig parent, BeamConfig root) {
        BeamList list = (BeamList) getParams().get(2);
        BeamResolvable var = getParams().get(0);
        String varId = var.getValue().toString();
        BeamResolvable resolvable = root.get(varId);
        boolean progress = false;
        int size = list.getList().size();
        if (!expanded) {
            List<BeamConfig> newList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                for (BeamConfig unResolvedConfig : getUnResolvedContext()) {
                    // need to find extensions based on type
                    BeamExtension extension = new ConfigExtension();

                    BeamConfig clone = extension.applyExtension(unResolvedConfig.getCtx());
                    newList.add(clone);
                }
            }

            setUnResolvedContext(newList);
            expanded = true;
        }

        int index = 0;
        int originSize = getUnResolvedContext().size() / size;
        for (BeamConfig unResolvedConfig : getUnResolvedContext()) {
            root.getContext().put(new BeamConfigKey(null, varId), list.getList().get(index ++ / originSize));
            if (unResolvedConfig.resolveParams(root)) {
                progress = unResolvedConfig.resolve(parent, root) || progress;
            }
        }

        if (resolvable != null) {
            root.getContext().put(new BeamConfigKey(null, varId), resolvable);
        } else {
            root.getContext().remove(new BeamConfigKey(null, varId));
        }

        return progress;
    }
}
