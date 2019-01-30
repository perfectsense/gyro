package beam.lang.ast.types;

import beam.lang.ast.Node;
import beam.lang.ast.Scope;

public class ValueReferenceNode extends Node {

    private final String name;

    public ValueReferenceNode(String name) {
        this.name = name;
    }

    @Override
    public Object evaluate(Scope scope) {
        for (Scope s = scope; s != null; s = s.getParent()) {
            if (s.containsKey(name)) {
                return s.get(name);
            }
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("$(");
        builder.append(name);
        builder.append(")");
    }
}
