package beam.lang.ast;

import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.tree.ParseTree;

public class KeyValueNode extends Node {

    private final String key;
    private final Node value;

    public KeyValueNode(BeamParser.KeyValueContext context) {
        ParseTree keyChild = context.key().getChild(0);
        key = (keyChild instanceof BeamParser.KeywordsContext ? keyChild.getChild(0) : keyChild).getText();
        value = Node.create(context.value().getChild(0));
    }

    public String getKey() {
        return key;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        scope.put(key, value.evaluate(scope));
        return scope.get(key);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(key);
        builder.append(": ");
        value.buildString(builder, indentDepth);
    }
}
