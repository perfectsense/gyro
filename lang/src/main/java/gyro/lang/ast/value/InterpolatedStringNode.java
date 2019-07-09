package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class InterpolatedStringNode extends Node {

    private final List<Node> items;

    public InterpolatedStringNode(List<Node> items) {
        this.items = ImmutableList.copyOf(Preconditions.checkNotNull(items));
    }

    public InterpolatedStringNode(GyroParser.InterpolatedStringContext context) {
        this(Node.create(context.stringContent()));
    }

    public List<Node> getItems() {
        return items;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitInterpolatedString(this, context);
    }

}
