package gyro.lang.ast;

import gyro.lang.ast.scope.FileScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.nio.file.Paths;
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

    public void load(Scope scope) throws Exception {
        FileScope parentFileScope = scope.getFileScope();
        FileScope fileRootScope = new FileScope(parentFileScope, file);

        parentFileScope.getImports().add(fileRootScope);

        if (!parentFileScope.getBackend().load(fileRootScope)) {
            throw new IllegalArgumentException(String.format(
                    "Can't find [%s]!",
                    file));
        }

        if (name != null) {
            if (name.equals("_")) {
                scope.putAll(fileRootScope);

            } else {
                scope.put(name, fileRootScope);
            }

        } else {
            scope.put(
                    Paths.get(fileRootScope.getFile())
                            .getFileName()
                            .toString()
                            .replace(".gyro", "")
                            .replace(".gyro.state", ""),
                    fileRootScope);
        }
    }

    @Override
    public Object evaluate(Scope scope) {
        throw new IllegalArgumentException();
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
