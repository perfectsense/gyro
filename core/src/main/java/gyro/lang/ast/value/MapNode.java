package gyro.lang.ast.value;

import gyro.lang.ast.KeyValueNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gyro.parser.antlr4.BeamParser.MapValueContext;
import static java.util.stream.Collectors.joining;

public class MapNode extends Node {

    private List<KeyValueNode> entries;

    public MapNode(List<KeyValueNode> entries) {
        this.entries = entries;
    }

    public MapNode(MapValueContext context) {
        entries = context.keyValue()
            .stream()
            .map(kv -> (KeyValueNode) Node.create(kv))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Scope bodyScope = new Scope(scope);
        Map<String, Object> map = new LinkedHashMap<>();

        for (KeyValueNode e : entries) {
            map.put(e.getKey(), e.evaluate(bodyScope));
        }

        return map;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('{');
        builder.append(entries.stream().map(Node::toString).collect(joining(", ")));
        builder.append('}');
    }

}
