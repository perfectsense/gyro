package beam.lang.ast;

import beam.parser.antlr4.BeamParser;
import org.apache.commons.lang.math.NumberUtils;

public class NumberNode extends Node {

    private final Number value;

    public NumberNode(BeamParser.NumberValueContext context) {
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
