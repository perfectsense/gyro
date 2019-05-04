package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

public class MapNode extends Node {

    private List<PairNode> entries;

    public MapNode(List<PairNode> entries) {
        this.entries = ImmutableList.copyOf(Preconditions.checkNotNull(entries));
    }

    public MapNode(GyroParser.MapContext context) {
        this(Preconditions.checkNotNull(context)
            .pair()
            .stream()
            .map(Node::create)
            .map(PairNode.class::cast)
            .collect(ImmutableCollectors.toList()));
    }

    public List<PairNode> getEntries() {
        return entries;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitMap(this, context);
    }

}
