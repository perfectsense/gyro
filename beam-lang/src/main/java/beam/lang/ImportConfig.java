package beam.lang;

import com.psddev.dari.util.StringUtils;

public class ImportConfig extends BeamConfig {

    private boolean imported;

    @Override
    protected boolean resolve(BeamConfig parent, BeamConfig root) {
        boolean progress = false;
        BeamResolvable pathResolvable = getParams().get(0);
        BeamResolvable idResolvable = getParams().get(2);
        String path = pathResolvable.getValue().toString();
        String id = idResolvable.getValue().toString();

        String statePath = StringUtils.ensureEnd(path, ".state");
        BeamConfigKey key = new BeamConfigKey(getType(), String.format("%s %s %s", statePath, "as", id));
        if (parent.getContext().containsKey(key)) {
            BeamConfig existingConfig = (BeamConfig) parent.getContext().get(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.getContext().put(key, this);
                progress = true;
            }
        } else {
            parent.getContext().put(key, this);
            progress = true;
        }

        if (!imported) {
            BeamConfig importConfig = BCL.parse(path);
            importConfig.applyExtension();
            root.getContext().put(new BeamConfigKey(null, id), importConfig);
            imported = true;
            progress = true;
        }

        return progress;
    }
}
