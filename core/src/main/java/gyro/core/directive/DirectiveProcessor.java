package gyro.core.directive;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gyro.core.GyroException;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

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

    public static List<Node> validateOptionArguments(DirectiveNode node, String name, int minimum, int maximum) {
        String errorName = String.format("@|bold @%s -%s|@ option", node.getName(), name);

        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .map(option -> validate(option, option.getArguments(), minimum, maximum, errorName))
            .orElseThrow(() -> new GyroException(node, String.format(
                "@|bold @%s|@ directive requires the @|bold -%s|@ option!",
                node.getName(),
                name)));
    }

    private static Node get(DirectiveNode node, int index) {
        List<Node> arguments = node.getArguments();
        return index < arguments.size() ? arguments.get(index) : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getArgument(Scope scope, DirectiveNode node, Class<T> valueClass, int index) {
        Node argument = get(node, index);

        if (argument == null) {
            return null;
        }

        Object value = scope.getRootScope().getEvaluator().visit(argument, scope);

        if (value == null) {
            throw new GyroException(argument, String.format(
                "Expected an instance of @|bold %s|@ at @|bold %s|@ but found a null!",
                valueClass.getName(),
                index));
        }

        if (!valueClass.isInstance(value)) {
            throw new GyroException(argument, String.format(
                "Expected an instance of @|bold %s|@ at @|bold %s|@ but found @|bold %s|@, an instance of @|bold %s|@!",
                valueClass.getName(),
                index,
                value,
                value.getClass().getName()));
        }

        return (T) value;
    }

    public static <T> List<T> getArguments(Scope scope, DirectiveNode node, Class<T> valueClass) {
        return IntStream.range(0, node.getArguments().size())
            .mapToObj(i -> getArgument(scope, node, valueClass, i))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getListArgument(Scope scope, DirectiveNode node, Class<T> itemClass, int index) {
        Node argument = get(node, index);

        if (argument == null) {
            return null;
        }

        Object value = scope.getRootScope().getEvaluator().visit(argument, scope);

        if (value == null) {
            throw new GyroException(argument, String.format(
                "Expected a list at @|bold %s|@ but found a null!",
                index));
        }

        if (!(value instanceof List)) {
            throw new GyroException(argument, String.format(
                "Expected a list at @|bold %s|@ but found @|bold %s|@, an instance of @|bold %s|@!",
                index,
                value,
                value.getClass().getName()));
        }

        List<?> list = (List<?>) value;

        for (int i = 0, s = list.size(); i < s; i++) {
            Object item = list.get(i);

            if (item == null) {
                throw new GyroException(argument, String.format(
                    "Expected an instance of @|bold %s|@ in the list at @|bold %s|@ but found a null!",
                    itemClass.getName(),
                    i));
            }

            if (!itemClass.isInstance(item)) {
                throw new GyroException(argument, String.format(
                    "Expected an instance of @|bold %s|@ in the list at @|bold %s|@ but found @|bold %s|@, an instance of @|bold %s|@!",
                    itemClass.getName(),
                    i,
                    item,
                    item.getClass().getName()));
            }
        }

        return (List<T>) value;
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
