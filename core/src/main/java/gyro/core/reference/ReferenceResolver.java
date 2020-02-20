/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.reference;

import java.util.ArrayList;
import java.util.List;

import gyro.core.GyroException;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ReferenceOption;

public abstract class ReferenceResolver {

    public abstract Object resolve(ReferenceNode node, Scope scope, List<Object> arguments) throws Exception;

    private static ReferenceOption getOption(ReferenceNode node, String name) {
        return node.getOptions()
            .stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    public static void validateOptionArguments(ReferenceNode node, String name, int minimum, int maximum) {
        ReferenceOption option = getOption(node, name);

        if (option != null) {
            validate(
                option,
                option.getArguments(),
                minimum,
                maximum,
                String.format("@|bold %s -%s|@ option", node.getArguments().get(0), name));

        } else if (minimum != 0) {
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
        ReferenceNode node,
        String name,
        Class<T> valueClass,
        int index) {
        ReferenceOption option = getOption(node, name);
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

    public static List<Node> validateArguments(ReferenceNode node, int minimum, int maximum) {
        List<Node> arguments = new ArrayList<>(node.getArguments());
        Node resolver = arguments.remove(0);
        return validate(
            node,
            arguments,
            minimum,
            maximum,
            String.format("@|bold %s|@ resolver", resolver));
    }

}
