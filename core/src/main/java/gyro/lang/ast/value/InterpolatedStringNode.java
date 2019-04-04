package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.List;
import java.util.stream.Collectors;

public class InterpolatedStringNode extends Node {

    private final List<Object> items;

    public InterpolatedStringNode(BeamParser.InterpolatedStringContext context) {
        items = context.stringContent()
                .stream()
                .map(c -> c.getChild(0))
                .map(c -> c instanceof BeamParser.ReferenceContext ? Node.create(c) : c.getText())
                .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
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
