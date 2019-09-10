package gyro.core.directive;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.DirectiveOption;

public abstract class DirectiveProcessor<S extends Scope> {

    private static List<Node> validate(Locatable locatable, List<Node> arguments, int minimum, int maximum, String errorName) {
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

            throw new GyroException(
                hasMaximum ? arguments.get(maximum) : locatable,
                String.format(
                    "%s requires %s arguments!",
                    errorName,
                    errorCount));
        }

        return arguments;
    }

    public static List<Node> validateArguments(DirectiveNode node, int minimum, int maximum) {
        return validate(node, node.getArguments(), minimum, maximum, String.format("@|bold @%s|@ directive", node.getName()));
    }

    private static DirectiveOption getOption(DirectiveNode node, String name) {
        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    public static List<Node> validateOptionArguments(DirectiveNode node, String name, int minimum, int maximum) {
        DirectiveOption option = getOption(node, name);

        if (option != null) {
            return validate(
                option,
                option.getArguments(),
                minimum,
                maximum,
                String.format("@|bold @%s -%s|@ option", node.getName(), name));

        } else if (minimum == 0) {
            return ImmutableList.of();

        } else {
            throw new GyroException(node, String.format(
                "@|bold @%s|@ directive requires the @|bold -%s|@ option!",
                node.getName(),
                name));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(Class<T> valueClass, Scope scope, List<Node> arguments, int index) {
        if (index < arguments.size()) {
            RootScope root = scope.getRootScope();
            return (T) root.convertValue(valueClass, root.getEvaluator().visit(arguments.get(index), scope));

        } else {
            return null;
        }
    }

    public static <T> T getArgument(Scope scope, DirectiveNode node, Class<T> valueClass, int index) {
        return convert(valueClass, scope, node.getArguments(), index);
    }

    public static <T> List<T> getArguments(Scope scope, DirectiveNode node, Class<T> valueClass) {
        return IntStream.range(0, node.getArguments().size())
            .mapToObj(i -> getArgument(scope, node, valueClass, i))
            .collect(Collectors.toList());
    }

    public static <T> T getOptionArgument(Scope scope, DirectiveNode node, String name, Class<T> valueClass, int index) {
        DirectiveOption option = getOption(node, name);
        return option != null ? convert(valueClass, scope, option.getArguments(), index) : null;
    }

    public static Scope evaluateBody(Scope scope, DirectiveNode node) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Scope bodyScope = new Scope(scope);

        evaluator.evaluateBody(node.getBody(), bodyScope);
        return bodyScope;
    }

    public abstract void process(S scope, DirectiveNode node) throws Exception;

}
