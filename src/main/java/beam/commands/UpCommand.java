package beam.commands;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import beam.core.BeamConfigLocation;
import beam.core.BeamException;
import beam.core.BeamRuntime;
import beam.core.diff.ChangeType;
import beam.core.diff.DiffUtil;
import beam.core.diff.ResourceDiff;
import beam.parser.ASTListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import beam.parser.ast.Node;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import org.fusesource.jansi.AnsiRenderWriter;

@Command(name = "up", description = "Updates all assets to match the configuration.")
public class UpCommand implements Runnable {

    @Arguments(title = "Beam config", usage = "<${config}.beam>", description = "Name of Beam config to use.")
    public String configName;

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public boolean debug;

    public void run() {
        parse(configName);
    }

    public ASTBeamRoot parse(String filename) {
        try {


            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            ASTListener listener = new ASTListener(filename);
            ParseTreeWalker.DEFAULT.walk(listener, context);
            return listener.getRoot();

        } catch (Exception error) {
            Throwable cause = null;

            PrintWriter out = new AnsiRenderWriter(System.out, true);
            if (error instanceof BeamException) {
                out.write("\n@|red Error: " + error.getMessage() + "|@\n");
                out.flush();

                if (debug) {
                    cause = error.getCause();
                }

            } else {
                out.write("\n@|red Unexpected error! Stack trace follows:|@\n");
                out.flush();

                cause = error;
            }

            if (cause != null) {
                out.write(cause.getClass().getName());
                out.write(": ");
                cause.printStackTrace(out);
                out.flush();
            }
        }

        return null;
    }
}
