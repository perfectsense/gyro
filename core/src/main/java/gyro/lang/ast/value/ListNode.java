package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListNode extends Node {

    private final List<Node> items;

    public ListNode(List<Node> items) {
        this.items = items;
    }

    public ListNode(BeamParser.ListValueContext context) {
        items = context.listItemValue()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }

    public List<Node> getItems() {
        return items;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        List<Object> list = new ArrayList<>();

        for (Node n : items) {
            list.add(n.evaluate(scope));
        }

        return list;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('[');
        builder.append(items.stream().map(Node::toString).collect(Collectors.joining(", ")));
        builder.append(']');
    }
}
