package beam.core;

import beam.lang.BCL;
import beam.lang.BeamConfig;
import beam.lang.ForConfig;

public class BeamCore {

    public static BeamConfig processConfig(String path) {
        BCL.getExtensions().clear();
        BCL.addExtension(new ForConfig());

        BeamConfig root = BCL.parse(path);
        root.applyExtension();
        BCL.resolve(root);

        BCL.addExtension(new BeamLocalState());
        root.applyExtension();

        BCL.addExtension(new BeamProvider());
        root.applyExtension();
        BCL.resolve(root);

        root.applyExtension();
        BCL.resolve(root);

        BCL.getDependencies(root);
        return root;
    }
}
