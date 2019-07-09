package gyro.core.directive;

import java.util.List;
import java.util.stream.Collectors;

import gyro.core.GyroException;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public abstract class DirectiveProcessor<S extends Scope> {

    private static List<Object> evaluateArguments(Scope scope, List<Node> arguments, int minimum, int maximum, String errorName) {
        int argumentsSize = arguments.size();
        boolean hasMinimum = minimum > 0;
        boolean hasMaximum = maximum > 0;

        if ((hasMinimum && argumentsSize < minimum) || (hasMaximum && maximum < argumentsSize)) {
            String errorCount;

            if (hasMinimum) {
                if (hasMaximum) {
                    if (minimum == maximum) {
                        errorCount = String.format("exactly @|bold %d|@", minimum);

                    } else {
                        errorCount = String.format("@|bold %d|@ to @|bold %d|@", minimum, maximum);
                    }

                } else {
                    errorCount = String.format("at least @|bold %d|@", minimum);
                }

            } else {
                errorCount = String.format("at most @|bold %d|@", maximum);
            }

            throw new GyroException(String.format(
                "%s requires %s arguments!",
                errorName,
                errorCount));
        }

        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        return arguments.stream()
            .map(a -> evaluator.visit(a, scope))
            .collect(Collectors.toList());
    }

    public static List<Object> evaluateDirectiveArguments(Scope scope, DirectiveNode node, int minimum, int maximum) {
        return evaluateArguments(scope, node.getArguments(), minimum, maximum, String.format("@|bold @%s|@ directive", node.getName()));
    }

    public static List<Object> evaluateOptionArguments(Scope scope, DirectiveNode node, String name, int minimum, int maximum) {
        String errorName = String.format("@|bold @%s -%s|@ option", node.getName(), name);

        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .map(n -> evaluateArguments(scope, n.getArguments(), minimum, maximum, errorName))
            .orElseThrow(() -> new GyroException(String.format(
                "@|bold @%s|@ directive requires the @|bold -%s|@ option!",
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
