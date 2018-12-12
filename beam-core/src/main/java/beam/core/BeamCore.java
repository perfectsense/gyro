package beam.core;

import beam.lang.BCL;
import beam.lang.BeamConfig;

import java.io.IOException;

public class BeamCore {

    private final BCL lang = new BCL();

    private static final BeamValidationException validationException = new BeamValidationException("Invalid config!");

    public static BeamValidationException validationException() {
        return validationException;
    }

    public BeamConfig processConfig(String path) throws IOException {
        lang.init();

        BeamConfig config = lang.parse(path);
        lang.applyExtension(config);
        lang.resolve(config);

        lang.addExtension("state", BeamLocalState.class);
        lang.addExtension("provider", BeamProvider.class);
        lang.applyExtension(config);
        lang.resolve(config);

        lang.applyExtension(config);
        lang.resolve(config);

        lang.getDependencies(config);
        return config;
    }
}
