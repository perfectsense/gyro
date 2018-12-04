package beam.core;

import beam.lang.BCL;
import beam.lang.BeamConfig;
import beam.lang.ForConfig;
import beam.lang.ImportConfig;

public class BeamCore {

    public static BeamConfig processConfig(String path) {
        BCL.getExtensions().clear();
        BCL.addExtension("for", ForConfig.class);
        BCL.addExtension("import", ImportConfig.class);

        BeamConfig root = BCL.parse(path);
        BCL.applyExtension();
        BCL.resolve();

        BCL.addExtension("state", BeamLocalState.class);
        BCL.applyExtension();

        BCL.addExtension("provider", BeamProvider.class);
        BCL.applyExtension();
        BCL.resolve();

        BCL.applyExtension();
        BCL.resolve();

        BCL.getDependencies();
        return root;
    }
}
