package gyro.lang.ast.control;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.BlockNode;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
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

    public List<String> getVariables() {
        return variables;
    }

    public List<Node> getItems() {
        return items;
    }

    @Override
    public <C> Object accept(NodeVisitor<C> visitor, C context) {
        return visitor.visitFor(this, context);
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
