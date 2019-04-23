package gyro.lang.ast.value;

import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.core.scope.Scope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gyro.parser.antlr4.GyroParser.MapContext;
import static java.util.stream.Collectors.joining;

public class MapNode extends Node {

    private List<PairNode> entries;

    public MapNode(List<PairNode> entries) {
        this.entries = entries;
    }

    public MapNode(MapContext context, String file) {
        entries = context.pair()
            .stream()
            .map(kv -> (PairNode) Node.create(kv, file))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Scope bodyScope = new Scope(scope);
        Map<String, Object> map = new LinkedHashMap<>();

        for (PairNode e : entries) {
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
