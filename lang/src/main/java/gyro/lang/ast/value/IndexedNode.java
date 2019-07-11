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
        super(null);

        this.value = Preconditions.checkNotNull(value);
        this.indexes = ImmutableList.copyOf(Preconditions.checkNotNull(indexes));
    }

    public IndexedNode(GyroParser.IndexedMulItemContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = Node.create(context.item());
        this.indexes = Node.create(context.index());
    }

    public Node getValue() {
        return value;
    }

    public List<Node> getIndexes() {
        return indexes;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitIndexed(this, context);
    }

}
