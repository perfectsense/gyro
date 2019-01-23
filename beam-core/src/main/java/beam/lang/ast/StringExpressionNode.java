package beam.lang.ast;

import beam.parser.antlr4.BeamParser;

import java.util.List;
import java.util.stream.Collectors;

public class StringExpressionNode extends Node {

    private final List<Object> items;

    public StringExpressionNode(BeamParser.StringExpressionContext context) {
        items = context.stringContents()
                .stream()
                .map(c -> c.getChild(0))
                .map(c -> c instanceof BeamParser.ReferenceBodyContext ? Node.create(c) : c.getText())
                .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) {
        StringBuilder builder = new StringBuilder();

        for (Object item : items) {
            if (item instanceof Node) {
                item = ((Node) item).evaluate(scope);
            }

            if (item != null) {
                builder.append(item);
            }
        }

        return builder.toString();
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('"');
        items.forEach(n -> builder.append(n.toString()));
        builder.append('"');
    }
}
