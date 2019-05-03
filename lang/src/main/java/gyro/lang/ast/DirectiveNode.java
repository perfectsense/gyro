package gyro.lang.ast;

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

    public String getDirective() {
        return directive;
    }

    public String getDirectiveFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitDirective(this, context);
    }

}
