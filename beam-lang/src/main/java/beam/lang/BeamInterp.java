package beam.lang;

import beam.lang.types.BeamBlock;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BeamInterp {

    private BeamBlock root;

    private final Map<String, Class<? extends BeamLanguageExtension>> extensions = new HashMap<>();

    private static final Formatter ui = new Formatter();

    public static Formatter ui() {
        return ui;
    }

    public void addExtension(String key, Class<? extends BeamLanguageExtension> extension) {
        extensions.put(key, extension);
    }

    public boolean hasExtension(String key) {
        return extensions.containsKey(key);
    }

    public Class<? extends BeamBlock> getExtension(String key) {
        return extensions.get(key);
    }

    public void init() {
        extensions.clear();
    }

    public BeamBlock parse(String filename) throws IOException {
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        BeamParser.Beam_rootContext context = parser.beam_root();

        BeamListener listener = new BeamListener(this);
        ParseTreeWalker.DEFAULT.walk(listener, context);

        root = listener.getRootBlock();
        if (!root.resolve()) {
            System.out.println("Unable to resolve config.");
        }

        return root;
    }

    public static void main(String[] arguments) throws Exception {
        BeamInterp interp = new BeamInterp();

        BeamBlock rootBlock = interp.parse(arguments[0]);
        System.out.println(rootBlock.toString());
    }

}
