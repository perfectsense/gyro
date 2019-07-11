package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class MapNode extends Node {

    private List<PairNode> entries;

    public MapNode(List<PairNode> entries) {
        super(null);

        this.entries = ImmutableList.copyOf(Preconditions.checkNotNull(entries));
    }

    @SuppressWarnings("unchecked")
    public MapNode(GyroParser.MapContext context) {
        super(Preconditions.checkNotNull(context));

        this.entries = (List) Node.create(context.pair());
    }

    public List<PairNode> getEntries() {
        return entries;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitMap(this, context);
    }

}
