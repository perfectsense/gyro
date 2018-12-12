package beam.core;

import beam.lang.BCL;
import beam.lang.BeamConfig;
import com.psddev.dari.util.ThreadLocalStack;

import java.io.IOException;

public class BeamCore {

    private static final ThreadLocalStack<BeamUI> UI = new ThreadLocalStack<>();

    private static final BeamValidationException validationException = new BeamValidationException("Invalid config!");

    private final BCL lang = new BCL();

    public static BeamUI ui() {
        return UI.get();
    }

    public static void pushUi(BeamUI ui) {
        UI.push(ui);
    }

    public static BeamUI popUi() {
        return UI.pop();
    }

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
