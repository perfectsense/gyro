package beam.lang.ast;

import beam.parser.antlr4.BeamParser;

import java.util.List;
import java.util.stream.Collectors;

public class KeyListValueNode extends Node {

    private final String key;
    private final List<Node> body;

    public KeyListValueNode(String key, List<BeamParser.SubresourceBodyContext> body) {
        this.key = key;

        this.body = body.stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) {
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        scope.addValue(key, bodyScope);
        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append(key);

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }
}
