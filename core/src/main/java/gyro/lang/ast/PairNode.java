package gyro.lang.ast;

import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class PairNode extends Node {

    private final String key;
    private final Node value;

    public PairNode(String key, Node value) {
        this.key = key;
        this.value = value;
    }

    public PairNode(GyroParser.PairContext context, String file) {
        key = context.key().getChild(0).getText();
        value = Node.create(context.value().getChild(0), file);
    }

    public String getKey() {
        return key;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        scope.put(key, value.evaluate(scope));
        scope.addValueNode(key, value);
        scope.getKeyNodes().put(key, this);
        return scope.get(key);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(key);
        builder.append(": ");
        value.buildString(builder, indentDepth);
    }
}
