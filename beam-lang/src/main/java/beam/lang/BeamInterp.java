package beam.lang;

import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BeamInterp {

    private final Map<String, Class<? extends BeamConfig>> extensions = new HashMap<>();

    private static final Formatter ui = new Formatter();

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

    public void init() {
        extensions.clear();
    }

    public BeamConfig parse(String filename) throws IOException {
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        BeamParser.BeamRootContext context = parser.beamRoot();

        File configFile = new File(filename);
        BeamListener listener = new BeamListener(configFile.getCanonicalPath());
        ParseTreeWalker.DEFAULT.walk(listener, context);

        return listener.getConfig();
    }

    public void resolve(BeamConfig config) {
        boolean progress = true;
        while (progress) {
            progress = config.resolve(config);
        }
    }

    public void applyExtension(BeamConfig config) {
        config.applyExtension(this);
    }

    public void getDependencies(BeamConfig config) {
        config.getDependencies(config);
    }
}
