package gyro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import gyro.lang.ast.OptionArgumentNode;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.value.Option;
import gyro.lang.ast.value.ReferenceNode;
import gyro.util.Bug;

public class OptionArgumentProcessor {
    private static Option getOption(OptionArgumentNode node, String name) {
        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    public static List<Node> validateOptionArguments(OptionArgumentNode node, String name, int minimum, int maximum) {
        Option option = getOption(node, name);
        List<String> typeAndValue = getTypeAndValue(node);

        if (option != null) {
            return validate(
                option,
                option.getArguments(),
                minimum,
                maximum,
                String.format("@|bold %s -%s|@ option", typeAndValue.get(0), name));

        } else if (minimum == 0) {
            return ImmutableList.of();

        } else {
            throw new GyroException(node, String.format(
                "@|bold %s|@ resolver requires the @|bold -%s|@ option!",
                node.getArguments().get(0),
                name));
        }
    }

    private static List<Node> validate(
        Locatable locatable,
        List<Node> arguments,
        int minimum,
        int maximum,
        String errorName) {
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

    public static <T> T getOptionArgument(
        Scope scope,
        OptionArgumentNode node,
        String name,
        Class<T> valueClass,
        int index) {
        Option option = getOption(node, name);
        return option != null ? convert(valueClass, scope, option.getArguments(), index) : null;
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

    public static List<Node> validateArguments(OptionArgumentNode node, int minimum, int maximum) {
        List<Node> arguments = new ArrayList<>(node.getArguments());

        // skip the first argument for reference node as that is the reference name
        if (node instanceof ReferenceNode) {
            arguments.remove(0);
        }

        List<String> typeAndValue = getTypeAndValue(node);
        return validate(
            node,
            arguments,
            minimum,
            maximum,
            String.format("@|bold %s|@ %s", typeAndValue.get(0), typeAndValue.get(1)));
    }

    public static <T> T getArgument(Scope scope, OptionArgumentNode node, Class<T> valueClass, int index) {
        return convert(valueClass, scope, node.getArguments(), index);
    }

    public static <T> List<T> getArguments(Scope scope, OptionArgumentNode node, Class<T> valueClass) {
        // skip the first argument for reference node as that is the reference name
        int start = node instanceof ReferenceNode ? 1 : 0;
        return IntStream.range(start, node.getArguments().size())
            .mapToObj(i -> getArgument(scope, node, valueClass, i))
            .collect(Collectors.toList());
    }

    // Find out if a resolver or directive is being processed.
    // Directives have the nme of the directive as a separate param.
    // Resolvers have the first argument as the name when the processor is involved.
    private static List<String> getTypeAndValue(OptionArgumentNode node) {
        List<String> typeAndValue = new ArrayList<>();

        if (node instanceof ReferenceNode) {
            typeAndValue.add(node.getArguments().get(0).toString());
            typeAndValue.add("resolver");
        } else if (node instanceof DirectiveNode) {
            typeAndValue.add(((DirectiveNode) node).getName());
            typeAndValue.add("directive");
        } else {
            throw new Bug(String.format("Unsupported OptionArgumentNode '%s' found!", node.getClass().getSimpleName()));
        }

        return typeAndValue;
    }
}
