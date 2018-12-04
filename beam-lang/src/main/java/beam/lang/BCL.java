package beam.lang;

import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import com.psddev.dari.util.ThreadLocalStack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class BCL {

    private static Map<String, Class<? extends BeamConfig>> extensions = new HashMap<>();

    private static final ThreadLocalStack<Formatter> UI = new ThreadLocalStack<>();

    private static final Map<String, BeamConfig> configs = new HashMap<>();

    public static Map<String, Class<? extends BeamConfig>> getExtensions() {
        return extensions;
    }

    public static Formatter ui() {
        return UI.get();
    }

    public static void addExtension(String alias, Class<? extends BeamConfig> extension) {
        getExtensions().put(alias, extension);
    }

    public static Map<String, BeamConfig> getConfigs() {
        return configs;
    }

    public static void init() {
        UI.push(new Formatter());
        BCL.addExtension("for", ForConfig.class);
        BCL.addExtension("import", ImportConfig.class);
    }

    public static void shutdown() {
        UI.pop();
    }

    public static BeamConfig parse(String filename) {
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

    public static void resolve() {
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

    public static void applyExtension() {
        for (BeamConfig config : getConfigs().values()) {
            config.applyExtension();
        }
    }

    public static void getDependencies() {
        for (BeamConfig config : getConfigs().values()) {
            config.getDependencies(config);
        }
    }
}
