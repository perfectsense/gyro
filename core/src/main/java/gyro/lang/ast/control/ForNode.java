package gyro.lang.ast.control;

import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.BlockNode;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;
import gyro.util.CascadingMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ForNode extends BlockNode {

    private final List<String> variables;
    private final List<Node> items;

    public ForNode(List<String> variables, List<Node> items, List<Node> body) {
        super(body);

        this.variables = variables;
        this.items = items;
    }

    public ForNode(GyroParser.ForStatementContext context) {
        super(context.blockBody()
                .blockStatement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

        variables = context.forVariable()
                .stream()
                .map(c -> c.IDENTIFIER().getText())
                .collect(Collectors.toList());

        items = context.list()
                .value()
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

            DeferError.evaluate(bodyScope, body);
            scope.getKeyNodes().putAll(bodyScope.getKeyNodes());
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
