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

}
