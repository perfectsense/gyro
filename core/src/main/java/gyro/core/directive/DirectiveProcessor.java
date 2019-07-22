package gyro.core.directive;

import java.util.List;
import java.util.stream.Collectors;

import gyro.core.GyroException;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public abstract class DirectiveProcessor<S extends Scope> {

    private static List<Node> validateArguments(Locatable locatable, List<Node> arguments, int minimum, int maximum, String errorName) {
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

            throw new GyroException(locatable, String.format(
                "%s requires %s arguments!",
                errorName,
                errorCount));
        }

        return arguments;
    }

    public static List<Node> validateDirectiveArguments(DirectiveNode node, int minimum, int maximum) {
        return validateArguments(node, node.getArguments(), minimum, maximum, String.format("@|bold @%s|@ directive", node.getName()));
    }

    public static List<Node> validateOptionArguments(DirectiveNode node, String name, int minimum, int maximum) {
        String errorName = String.format("@|bold @%s -%s|@ option", node.getName(), name);

        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .map(option -> validateArguments(option, option.getArguments(), minimum, maximum, errorName))
            .orElseThrow(() -> new GyroException(node, String.format(
                "@|bold @%s|@ directive requires the @|bold -%s|@ option!",
                node.getName(),
                name)));
    }

    private static List<Object> evaluateArguments(Scope scope, List<Node> arguments) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        return arguments.stream()
            .map(a -> evaluator.visit(a, scope))
            .collect(Collectors.toList());
    }

    public static List<Object> evaluateDirectiveArguments(Scope scope, DirectiveNode node, int minimum, int maximum) {
        return evaluateArguments(scope, validateDirectiveArguments(node, minimum, maximum));
    }

    public static List<Object> evaluateOptionArguments(Scope scope, DirectiveNode node, String name, int minimum, int maximum) {
        return evaluateArguments(scope, validateOptionArguments(node, name, minimum, maximum));
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
