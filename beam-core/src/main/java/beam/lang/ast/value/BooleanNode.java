package beam.lang.ast.value;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

public class BooleanNode extends Node {

    private final boolean value;

    public BooleanNode(boolean value) {
        this.value = value;
    }

    public BooleanNode(BeamParser.BooleanValueContext context) {
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
