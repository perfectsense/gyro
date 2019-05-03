package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class ListNode extends Node {

    private final List<Node> items;

    public ListNode(List<Node> items) {
        this.items = items;
    }

    public ListNode(GyroParser.ListContext context) {
        items = context.value()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }

    public List<Node> getItems() {
        return items;
    }

    @Override
    public <C> Object accept(NodeVisitor<C> visitor, C context) {
        return visitor.visitList(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('[');
        builder.append(items.stream().map(Node::toString).collect(Collectors.joining(", ")));
        builder.append(']');
    }
}
