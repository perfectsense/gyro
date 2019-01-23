package beam.lang.ast;

import beam.core.BeamException;
import beam.lang.listeners.ErrorListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ImportNode extends Node {

    private final String path;
    private final String name;

    public ImportNode(BeamParser.ImportStmtContext context) {
        path = context.importPath().IDENTIFIER().getText();

        name = Optional.ofNullable(context.importName())
                .map(c -> c.IDENTIFIER().getText())
                .orElse(null);
    }

    @Override
    public Object evaluate(Scope scope) {
        Path file = Paths.get((String) scope.get("_file")).getParent().resolve(path);

        if (!file.endsWith(".bcl") && !file.endsWith(".bcl.state")) {
            file = Paths.get(file.toString() + ".bcl");
        }

        BeamLexer lexer;

        try {
            lexer = new BeamLexer(CharStreams.fromFileName(file.toString()));

        } catch (IOException error) {
            throw new BeamException(String.format("Can't import [%s]!", file));
        }

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BeamParser parser = new BeamParser(tokens);
        ErrorListener errorListener = new ErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        Node rootNode = Node.create(parser.beamFile());
        Scope rootScope = new Scope(null);

        rootScope.put("_file", file.toString());
        rootNode.evaluate(rootScope);

        if (name != null) {
            if (name.equals("_")) {
                scope.putAll(rootScope);

            } else {
                scope.put(name, rootScope);
            }

        } else {
            scope.put(
                    file.getFileName()
                            .toString()
                            .replace(".bcl", "")
                            .replace(".bcl.state", ""),
                    rootScope);
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append("import ");
        builder.append(path);

        if (name != null) {
            builder.append(" as ");
            builder.append(name);
        }
    }
}
