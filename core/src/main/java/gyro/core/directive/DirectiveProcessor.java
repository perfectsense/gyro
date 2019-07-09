package gyro.core.directive;

import java.util.List;
import java.util.stream.Collectors;

import gyro.core.GyroException;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public abstract class DirectiveProcessor<S extends Scope> {

    public static List<Object> evaluateArguments(Scope scope, DirectiveNode node) {
        return evaluateArguments(scope, node.getArguments());
    }

    public static List<Object> evaluateArguments(Scope scope, List<Node> arguments) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        return arguments.stream()
            .map(a -> evaluator.visit(a, scope))
            .collect(Collectors.toList());
    }

    public static List<Object> evaluateOptionArguments(Scope scope, DirectiveNode node, String name) {
        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .map(n -> evaluateArguments(scope, n.getArguments()))
            .orElseThrow(() -> new GyroException(String.format(
                "@%s directive requires -%s option!",
                node.getName(),
                name)));
    }

    public static Scope evaluateBody(Scope scope, DirectiveNode node) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Scope bodyScope = new Scope(scope);

        evaluator.visitBody(node.getBody(), bodyScope);
        return bodyScope;
    }

    public abstract String getName();

    public abstract void process(S scope, DirectiveNode node) throws Exception;

}
