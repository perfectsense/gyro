package beam.lang.ast.types;

import beam.lang.ast.KeyValueNode;
import beam.lang.ast.Node;
import beam.lang.ast.Scope;

import java.util.List;
import java.util.stream.Collectors;

import static beam.parser.antlr4.BeamParser.MapValueContext;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class MapNode extends Node {

    private List<KeyValueNode> entries;

    public MapNode(MapValueContext context) {
        entries = context.keyValue()
            .stream()
            .map(kv -> (KeyValueNode) Node.create(kv))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) {
        Scope bodyScope = new Scope(scope);

        return entries
            .stream()
            .collect(toMap(
                e -> e.getKey(),
                e -> e.evaluate(bodyScope)
            ));
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('{');
        builder.append(entries.stream().map(Node::toString).collect(joining(", ")));
        builder.append('}');
    }

}
