package gyro.lang.ast.value;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class IndexedNode extends Node {

    private final Node value;
    private final List<Node> indexes;

    public IndexedNode(Node value, List<Node> indexes) {
        this.value = Preconditions.checkNotNull(value);
        this.indexes = ImmutableList.copyOf(Preconditions.checkNotNull(indexes));
    }

    public IndexedNode(GyroParser.IndexedMulItemContext context) {
        this(
            Node.create(Preconditions.checkNotNull(context).item()),
            Node.create(context.index()));
    }

    public Node getValue() {
        return value;
    }

    public List<Node> getIndexes() {
        return indexes;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitIndexedNode(this, context);
    }

}
