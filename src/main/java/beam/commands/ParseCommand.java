package beam.commands;

import java.io.IOException;

import beam.parser.ASTListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import beam.parser.ast.Node;
import io.airlift.airline.Command;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

@Command(name = "parse", description = "Parse")
public class ParseCommand implements Runnable {

    public void run() {
        ASTBeamRoot root = parse("test.beam");
        if (root == null) {
            return;
        }

        for (Node n : root.getNodes()) {
            System.out.println(n);
        }
    }

    public static ASTBeamRoot parse(String filename) {
        try {
            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            ASTListener listener = new ASTListener();
            ParseTreeWalker.DEFAULT.walk(listener, context);

            return listener.getRoot();

        } catch (IOException ioe) {

        }

        return null;
    }

}
