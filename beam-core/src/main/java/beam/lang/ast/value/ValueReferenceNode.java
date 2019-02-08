package beam.lang.ast.value;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;

public class ValueReferenceNode extends Node {

    private final String path;

    public ValueReferenceNode(String path) {
        this.path = path;
    }

    @Override
    public Object evaluate(Scope scope) {
        return scope.find(path);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("$(");
        builder.append(path);
        builder.append(")");
    }
}
