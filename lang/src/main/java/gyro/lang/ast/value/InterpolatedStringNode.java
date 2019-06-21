package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

public class InterpolatedStringNode extends Node {

    private final List<Node> items;

    public InterpolatedStringNode(List<Node> items) {
        this.items = ImmutableList.copyOf(Preconditions.checkNotNull(items));
    }

    public InterpolatedStringNode(GyroParser.InterpolatedStringContext context) {
        this(context.stringContent()
            .stream()
            .map(c -> Node.create(c.getChild(0)))
            .collect(ImmutableCollectors.toList()));
    }

    public List<Node> getItems() {
        return items;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitInterpolatedString(this, context);
    }

}
