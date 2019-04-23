package gyro.lang.ast.control;

import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.condition.ConditionNode;
import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class IfNode extends Node {

    private final List<Node> conditions;
    private final List<List<Node>> bodies;

    public IfNode(GyroParser.IfStatementContext context, String file) {
        conditions = context.condition()
            .stream()
            .map(e -> Node.create(e, file))
            .collect(Collectors.toList());

        bodies = context.blockBody()
            .stream()
            .map(cbc -> cbc.blockStatement()
                .stream()
                .map(csc -> Node.create(csc.getChild(0), file))
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        for (int i = 0; i < conditions.size(); i++) {
            Node expression = conditions.get(i);
            Boolean value = ConditionNode.toBoolean(expression.evaluate(scope));

            if (value) {
                DeferError.evaluate(scope, bodies.get(i));
                return null;
            }
        }

        if (bodies.size() > conditions.size()) {
            DeferError.evaluate(scope, bodies.get(bodies.size() - 1));
        }

        return null;
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
