package beam.core;

import beam.lang.BCL;
import beam.lang.BeamConfig;

import java.util.Map;

public class BeamCore {

    private final BCL lang = new BCL();

    private static final BeamValidationException validationException = new BeamValidationException("Invalid config!");

    public static BeamValidationException validationException() {
        return validationException;
    }

    public void processConfig(String path) {
        lang.init();

        lang.parse(path);
        lang.applyExtension();
        lang.resolve();

        lang.addExtension("state", BeamLocalState.class);
        lang.addExtension("provider", BeamProvider.class);
        lang.applyExtension();
        lang.resolve();

        lang.applyExtension();
        lang.resolve();

        lang.getDependencies();
    }

    public Map<String, BeamConfig> getConfigs() {
        return lang.getConfigs();
    }
}
