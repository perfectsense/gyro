package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.apache.commons.lang.math.NumberUtils;

public class NumberNode extends Node {

    private final Number value;

    public NumberNode(Number value) {
        this.value = value;
    }

    public NumberNode(GyroParser.NumberContext context) {
        value = NumberUtils.createNumber(context.getText());
    }

    public Number getValue() {
        return value;
    }

    @Override
    public <C> Object accept(NodeVisitor<C> visitor, C context) {
        return visitor.visitNumber(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(value);
    }
}
