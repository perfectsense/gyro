package beam.lang.ast;

import org.antlr.v4.runtime.tree.ParseTree;

public class UnknownNode extends Node {

    private final ParseTree context;

    public UnknownNode(ParseTree context) {
        this.context = context;
    }

    @Override
    public Object evaluate(Scope scope) {
        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("UNKNOWN (");
        builder.append(context.getClass().getName());
        builder.append(")");
    }
}
