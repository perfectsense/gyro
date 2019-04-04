package gyro.lang.ast;

import java.util.Optional;

import gyro.lang.ast.scope.DiffableScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

public class KeyValueNode extends Node {

    private final String key;
    private final Node value;

    public KeyValueNode(String key, Node value) {
        this.key = key;
        this.value = value;
    }

    public KeyValueNode(BeamParser.KeyValueStatementContext context) {
        key = context.key().getChild(0).getText();
        value = Node.create(context.value().getChild(0));
    }

    public String getKey() {
        return key;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Optional.ofNullable(scope.getClosest(DiffableScope.class))
                .ifPresent(s -> s.add(key, value, scope));

        scope.put(key, value.evaluate(scope));
        scope.addValueNode(key, value);
        return scope.get(key);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(key);
        builder.append(": ");
        value.buildString(builder, indentDepth);
    }
}
