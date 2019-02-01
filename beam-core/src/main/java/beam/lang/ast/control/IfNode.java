package beam.lang.ast.control;

import beam.lang.ast.DeferError;
import beam.lang.ast.Node;
import beam.lang.ast.expression.ExpressionNode;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

import java.util.List;
import java.util.stream.Collectors;

public class IfNode extends Node {

    private final List<Node> expressions;
    private final List<List<Node>> bodies;

    public IfNode(BeamParser.IfStmtContext context) {
        expressions = context.expression()
            .stream()
            .map(e -> Node.create(e))
            .collect(Collectors.toList());

        bodies = context.controlBody()
            .stream()
            .map(cbc -> cbc.controlStmts()
                .stream()
                .map(csc -> Node.create(csc.getChild(0)))
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        for (int i = 0; i < expressions.size(); i++) {
            Node expression = expressions.get(i);
            Boolean value = ExpressionNode.toBoolean(expression.evaluate(scope));

            if (value) {
                DeferError.evaluate(scope, bodies.get(i));
                return null;
            }
        }

        if (bodies.size() > expressions.size()) {
            DeferError.evaluate(scope, bodies.get(bodies.size() - 1));
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        for (int i = 0; i < expressions.size(); i++) {
            builder.append(i == 0 ? "if " : "else if ");
            builder.append(expressions.get(i));
            buildBody(builder, indentDepth + 1, bodies.get(i));
            buildNewline(builder, indentDepth);
        }

        if (bodies.size() > expressions.size()) {
            buildNewline(builder, indentDepth);
            builder.append("else");
            buildBody(builder, indentDepth + 1, bodies.get(bodies.size() - 1));
        }

        buildNewline(builder, indentDepth);
        builder.append("end");
    }

}
