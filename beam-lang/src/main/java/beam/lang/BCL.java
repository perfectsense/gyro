package beam.lang;

import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class BCL {

    private final Map<String, Class<? extends BeamConfig>> extensions = new HashMap<>();

    private static final Formatter ui = new Formatter();

    private final Map<String, BeamConfig> configs = new HashMap<>();

    public static Formatter ui() {
        return ui;
    }

    public void addExtension(String key, Class<? extends BeamConfig> extension) {
        extensions.put(key, extension);
    }

    public boolean hasExtension(String key) {
        return extensions.containsKey(key);
    }

    public Class<? extends BeamConfig> getExtension(String key) {
        return extensions.get(key);
    }

    public Map<String, BeamConfig> getConfigs() {
        return configs;
    }

    public void init() {
        extensions.clear();
        configs.clear();
        addExtension("for", ForExtension.class);
        addExtension("import", ImportExtension.class);
    }

    public BeamConfig parse(String filename) {
        try {
            if (getConfigs().containsKey(filename)) {
                return getConfigs().get(filename);
            }

            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            File configFile = new File(filename);
            BeamConfig passingContext = new BeamConfig();

            BeamListener listener = new BeamListener(configFile.getCanonicalPath(), passingContext);
            ParseTreeWalker.DEFAULT.walk(listener, context);
            getConfigs().put(filename, listener.getConfig());
            return listener.getConfig();

        } catch (Exception error) {
            error.printStackTrace();
        }

        return new BeamConfig();
    }

    public void resolve() {
        boolean progress = true;
        while (progress) {
            progress = false;
            List<BeamConfig> configs = new ArrayList<>();
            configs.addAll(getConfigs().values());
            for (BeamConfig root : configs) {
                progress = root.resolve(root) || progress;
            }
        }
    }

    public void applyExtension() {
        for (BeamConfig config : getConfigs().values()) {
            config.applyExtension(this);
        }
    }

    public void getDependencies() {
        for (BeamConfig config : getConfigs().values()) {
            config.getDependencies(config);
        }
    }
}
