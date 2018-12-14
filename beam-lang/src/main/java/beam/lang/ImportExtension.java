package beam.lang;

import com.psddev.dari.util.StringUtils;

import java.io.IOException;

public class ImportExtension extends BeamBlockMethod {

    private boolean imported;

    @Override
    protected boolean resolve(BeamContext parent, BeamContext root) {
        boolean progress = false;
        BeamResolvable pathResolvable = getParams().get(0);
        BeamResolvable idResolvable = getParams().get(2);
        String path = pathResolvable.getValue().toString();
        String id = idResolvable.getValue().toString();

        String statePath = StringUtils.ensureEnd(path, ".state");
        BeamContextKey key = new BeamContextKey(String.format("%s %s %s", statePath, "as", id), getType());
        if (parent.containsKey(key)) {
            BeamBlock existingConfig = (BeamBlock) parent.get(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.add(key, this);
                progress = true;
            }
        } else {
            parent.add(key, this);
            progress = true;
        }

        if (!imported) {
            try {
                BeamBlock importConfig = getInterp().parse(path);
                importConfig.applyExtension(getInterp());
                root.add(new BeamContextKey(id), importConfig);
                imported = true;
                progress = true;
            } catch (IOException ioe) {
                throw new BeamLangException("Unable to import '" + path + "'.");
            }
        }

        return progress;
    }
}
