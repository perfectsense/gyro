package gyro.lang.ast.value;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class InterpolatedStringNode extends Node {

    private final List<Object> items;

    public InterpolatedStringNode(GyroParser.InterpolatedStringContext context) {
        items = context.stringContent()
                .stream()
                .map(c -> c.getChild(0))
                .map(c -> c instanceof GyroParser.ReferenceContext ? Node.create(c) : c.getText())
                .collect(Collectors.toList());
    }

    public List<Object> getItems() {
        return items;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitInterpolatedString(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('"');
        items.forEach(n -> builder.append(n.toString()));
        builder.append('"');
    }
}
