package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class BooleanNode extends Node {

    private final boolean value;

    public BooleanNode(boolean value) {
        this.value = value;
    }

    public BooleanNode(GyroParser.BooleanValueContext context) {
        value = context.TRUE() != null;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitBoolean(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(value);
    }
}
