package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import org.apache.commons.lang.math.NumberUtils;

public class NumberNode extends Node {

    private final Number value;

    public NumberNode(Number value) {
        this.value = value;
    }

    public NumberNode(BeamParser.NumberContext context) {
        value = NumberUtils.createNumber(context.getText());
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
