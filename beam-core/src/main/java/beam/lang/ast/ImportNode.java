package beam.lang.ast;

import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

import java.util.Optional;

public class ImportNode extends Node {

    private final String file;
    private final String name;

    public ImportNode(BeamParser.ImportStmtContext context) {
        file = context.importPath().IDENTIFIER().getText();

        name = Optional.ofNullable(context.importName())
                .map(c -> c.IDENTIFIER().getText())
                .orElse(null);
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        FileScope parentFileScope = scope.getFileScope();
        FileScope fileRootScope = scope.getFileScope().getStateBackend().load(scope.getFileScope(), file);

        parentFileScope.getImports().add(fileRootScope);

        if (name != null) {
            if (name.equals("_")) {
                for (String key : fileRootScope.keySet()) {
                    if (!key.startsWith("_")) {
                        scope.put(key, fileRootScope.get(key));
                    }
                }
            } else {
                scope.put(name, fileRootScope);
            }

        } else {
            scope.put(
                    fileRootScope.getFile()
                            .replace(".bcl", "")
                            .replace(".bcl.state", ""),
                    fileRootScope);
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append("import ");
        builder.append(file);

        if (name != null) {
            builder.append(" as ");
            builder.append(name);
        }
    }
}
