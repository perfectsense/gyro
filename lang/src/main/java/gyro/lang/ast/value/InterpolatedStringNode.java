package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

public class InterpolatedStringNode extends Node {

    private final List<Object> items;

    public InterpolatedStringNode(List<Object> items) {
        Preconditions.checkNotNull(items);

        Preconditions.checkArgument(
            items.stream().allMatch(i -> i instanceof Node || i instanceof String),
            "All items must be an instance of Node or String!");

        this.items = ImmutableList.copyOf(items);
    }

    public InterpolatedStringNode(GyroParser.InterpolatedStringContext context) {
        this(context.stringContent()
            .stream()
            .map(c -> c.getChild(0))
            .map(c -> c instanceof GyroParser.ReferenceContext ? Node.create(c) : c.getText())
            .collect(ImmutableCollectors.toList()));
    }

    public List<Object> getItems() {
        return items;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitInterpolatedString(this, context);
    }

}
