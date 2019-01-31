package beam.lang.ast.controls;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;
import beam.util.CascadingMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ForNode extends Node {

    private final List<String> variables;
    private final List<Node> items;
    private final List<Node> body;

    public ForNode(List<String> variables, List<Node> items, List<Node> body) {
        this.variables = variables;
        this.items = items;
        this.body = body;
    }

    public ForNode(BeamParser.ForStmtContext context) {
        variables = context.forVariables()
                .forVariable()
                .stream()
                .map(c -> c.IDENTIFIER().getText())
                .collect(Collectors.toList());

        items = context.listValue()
                .listItemValue()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());

        body = context.controlBody()
                .controlStmts()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        int variablesSize = variables.size();
        int itemsSize = items.size();

        for (int i = 0; i < itemsSize; i += variablesSize) {
            Map<String, Object> values = new LinkedHashMap<>();
            Scope bodyScope = new Scope(scope, new CascadingMap<>(scope, values));

            for (int j = 0; j < variablesSize; j++) {
                int k = i + j;

                values.put(
                        variables.get(j),
                        k < itemsSize
                                ? items.get(k).evaluate(scope)
                                : null);
            }

            for (Node node : body) {
                node.evaluate(bodyScope);
            }
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append("for ");
        builder.append(String.join(", ", variables));
        builder.append(" in [");
        builder.append(items.stream().map(Node::toString).collect(Collectors.joining(", ")));
        builder.append("]");

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }
}
