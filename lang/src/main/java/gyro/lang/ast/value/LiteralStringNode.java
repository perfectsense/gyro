package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.apache.commons.lang3.StringUtils;

public class LiteralStringNode extends Node {

    private final String value;

    public LiteralStringNode(String value) {
        this.value = value;
    }

    public LiteralStringNode(GyroParser.LiteralStringContext context) {
        this(StringUtils.strip(context.STRING().getText(), "'"));
    }

    public String getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitLiteralString(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('\"');
        builder.append(value);
        builder.append('\"');
    }
}
