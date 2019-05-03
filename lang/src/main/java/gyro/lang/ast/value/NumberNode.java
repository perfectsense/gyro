package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.apache.commons.lang3.math.NumberUtils;

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
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitNumber(this, context);
    }

}
