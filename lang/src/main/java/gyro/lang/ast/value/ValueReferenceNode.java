package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class ValueReferenceNode extends Node {

    private final String path;

    public ValueReferenceNode(String path) {
        this.path = path;
    }

    public ValueReferenceNode(GyroParser.ValueReferenceContext context) {
        this(context.path().getText());
    }

    public String getPath() {
        return path;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitValueRefence(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("$(");
        builder.append(path);
        builder.append(")");
    }
}
