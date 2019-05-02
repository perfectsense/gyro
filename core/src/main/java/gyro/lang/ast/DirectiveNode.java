package gyro.lang.ast;

import gyro.core.GyroException;
import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.Optional;

public class DirectiveNode extends Node {

    private final String directive;
    private final String file;
    private final String name;

    public DirectiveNode(GyroParser.DirectiveContext context) {
        directive = context.IDENTIFIER().getText();
        file = context.directiveArgument(0).getText();

        name = Optional.ofNullable(context.directiveArgument(2))
                .map(GyroParser.DirectiveArgumentContext::getText)
                .orElse(null);
    }

    @Override
    public Object evaluate(Scope scope) {
        throw new GyroException(String.format("[%s] directive isn't supported!", directive));
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
