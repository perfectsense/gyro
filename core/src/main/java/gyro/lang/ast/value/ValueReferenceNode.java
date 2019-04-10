package gyro.lang.ast.value;

import gyro.core.BeamException;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;

public class ValueReferenceNode extends Node {

    private final String path;

    public ValueReferenceNode(String path) {
        this.path = path;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        try {
            return scope.find(path);
        } catch (ValueReferenceException vre) {
            throw new BeamException(String.format("Unable to resolve value reference %s %s%n'%s' is not defined.%n",
                this, this.getLocation(), vre.getKey()));
        }
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("$(");
        builder.append(path);
        builder.append(")");
    }
}
