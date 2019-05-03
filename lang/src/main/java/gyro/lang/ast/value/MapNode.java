package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class MapNode extends Node {

    private List<PairNode> entries;

    public MapNode(List<PairNode> entries) {
        this.entries = entries;
    }

    public MapNode(GyroParser.MapContext context) {
        entries = context.pair()
            .stream()
            .map(kv -> (PairNode) Node.create(kv))
            .collect(Collectors.toList());
    }

    public List<PairNode> getEntries() {
        return entries;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitMap(this, context);
    }

}
