package gyro.lang.ast.control;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class IfNode extends Node {

    private final List<Node> conditions;
    private final List<List<Node>> bodies;

    public IfNode(GyroParser.IfStatementContext context) {
        conditions = context.condition()
            .stream()
            .map(e -> Node.create(e))
            .collect(Collectors.toList());

        bodies = context.blockBody()
            .stream()
            .map(cbc -> cbc.blockStatement()
                .stream()
                .map(csc -> Node.create(csc.getChild(0)))
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    public List<Node> getConditions() {
        return conditions;
    }

    public List<List<Node>> getBodies() {
        return bodies;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitIf(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        for (int i = 0; i < conditions.size(); i++) {
            builder.append(i == 0 ? "if " : "else if ");
            builder.append(conditions.get(i));
            buildBody(builder, indentDepth + 1, bodies.get(i));
            buildNewline(builder, indentDepth);
        }

        if (bodies.size() > conditions.size()) {
            buildNewline(builder, indentDepth);
            builder.append("else");
            buildBody(builder, indentDepth + 1, bodies.get(bodies.size() - 1));
        }

        buildNewline(builder, indentDepth);
        builder.append("end");
    }

}
