package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;

import java.util.List;
import java.util.stream.Collectors;

import static gyro.parser.antlr4.GyroParser.MapContext;
import static java.util.stream.Collectors.joining;

public class MapNode extends Node {

    private List<PairNode> entries;

    public MapNode(List<PairNode> entries) {
        this.entries = entries;
    }

    public MapNode(MapContext context) {
        entries = context.pair()
            .stream()
            .map(kv -> (PairNode) Node.create(kv))
            .collect(Collectors.toList());
    }

    public List<PairNode> getEntries() {
        return entries;
    }

    @Override
    public <C> Object accept(NodeVisitor<C> visitor, C context) {
        return visitor.visitMap(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('{');
        builder.append(entries.stream().map(Node::toString).collect(joining(", ")));
        builder.append('}');
    }

}
