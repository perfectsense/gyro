package gyro.lang.ast;

import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.Optional;

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
        throw new UnsupportedOperationException();
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
