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
    protected boolean resolve(BeamContext parent, BeamContext root) {
        BeamList list = (BeamList) getParams().get(2);
        BeamResolvable var = getParams().get(0);
        String varId = var.getValue().toString();
        BeamReferable referable = root.getReferable(new BeamContextKey(null, varId));
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
            root.addReferable(new BeamContextKey(null, varId), list.getList().get(index ++ / originSize));
            if (unResolvedConfig.resolveParams(root)) {
                progress = unResolvedConfig.resolve(parent, root) || progress;
            }
        }

        if (referable != null) {
            root.addReferable(new BeamContextKey(null, varId), referable);
        } else {
            root.removeReferable(new BeamContextKey(null, varId));
        }

        return progress;
    }
}
