package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class BooleanNode extends Node {

    private final boolean value;

    public BooleanNode(boolean value) {
        this.value = value;
    }

    public BooleanNode(GyroParser.BooleanValueContext context) {
        value = context.TRUE() != null;
    }

    @Override
    public Object evaluate(Scope scope) {
        return value;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(value);
    }
}
