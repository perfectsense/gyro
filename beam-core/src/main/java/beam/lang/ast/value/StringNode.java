package beam.lang.ast.value;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;

public class StringNode extends Node {

    private final String value;

    public StringNode(String value) {
        this.value = value;
    }

    @Override
    public Object evaluate(Scope scope) {
        return value;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('\'');
        builder.append(value);
        builder.append('\'');
    }
}
