package gyro.lang.ast;

import gyro.lang.Resource;
import gyro.lang.ast.scope.FileScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DirectiveNode extends Node {

    private final String file;
    private final String name;

    public DirectiveNode(GyroParser.DirectiveContext context) {
        String directive = context.IDENTIFIER().getText();

        if (!"import".equals(directive)) {
            throw new IllegalArgumentException(
                String.format("[%s] isn't a valid directive!", directive));
        }

        file = context.directiveArgument(0).getText();

        name = Optional.ofNullable(context.directiveArgument(2))
                .map(GyroParser.DirectiveArgumentContext::getText)
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
                scope.putAll(fileRootScope.entrySet()
                    .stream()
                    .filter(e -> !(e.getValue() instanceof Resource))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

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
        builder.append("@import ");
        builder.append(file);

        if (name != null) {
            builder.append(" as ");
            builder.append(name);
        }
    }

}
