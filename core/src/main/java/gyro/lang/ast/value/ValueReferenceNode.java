package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;

public class ValueReferenceNode extends Node {

    private final String path;

    public ValueReferenceNode(String path) {
        this.path = path;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        return scope.find(path, this);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("$(");
        builder.append(path);
        builder.append(")");
    }
}
