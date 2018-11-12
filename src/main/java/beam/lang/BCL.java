package beam.lang;

import beam.core.BeamUI;
import beam.core.CLIBeamUI;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import com.psddev.dari.util.ThreadLocalStack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BCL {

    private static Map<String, BeamExtension> extensions = new HashMap<>();

    private static final ThreadLocalStack<BeamUI> UI = new ThreadLocalStack<>();

    public static Map<String, BeamExtension> getExtensions() {
        return extensions;
    }

    public static BeamUI ui() {
        return UI.get();
    }

    public static void addExtension(BeamExtension extension) {
        String name = extension.getName();
        getExtensions().put(name, extension);
    }


    public static void init() {
        UI.push(new CLIBeamUI());
        BCL.addExtension(new ConfigExtension());
    }

    public static void shutdown() {
        UI.pop();
    }

    public static BeamConfig parse(String filename) {
        try {
            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            File configFile = new File(filename);
            BeamConfig passingContext = new BeamConfig();

            BeamListener listener = new BeamListener(configFile.getCanonicalPath(), passingContext);
            ParseTreeWalker.DEFAULT.walk(listener, context);
            return listener.getConfig();

        } catch (Exception error) {
            error.printStackTrace();
        }

        return new BeamConfig();
    }

    public static void resolve(BeamConfig root) {
        boolean progress = true;
        while (progress) {
            progress = root.resolve(root);
        }
    }
}
