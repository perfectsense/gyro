package gyro.core.scope;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.value.ValueNode;

public class ScopingPolice {

    public static List<String> getNodeVariables(List<Node> nodes) {
        return nodes.stream()
            .filter(o -> o instanceof PairNode)
            .map(o -> (String) ((ValueNode) ((PairNode) o).getKey()).getValue())
            .collect(Collectors.toList());
    }

    public static Node getKeyNode(List<Node> nodes, String key) {
        return nodes.stream()
            .filter(o -> o instanceof PairNode)
            .filter(o -> ((ValueNode) ((PairNode) o).getKey()).getValue().equals(
                key))
            .findFirst()
            .orElse(null);
    }

    public static String validateLocalImmutabilityVariables(List<String> nodeVariables) {
        return nodeVariables.stream()
            .filter(e -> Collections.frequency(nodeVariables, e) > 1)
            .findFirst().orElse(null);
    }

    public static String validateLocalImmutability(List<Node> nodes) {
        return validateLocalImmutabilityVariables(getNodeVariables(nodes));
    }

    public static String validateGlobalImmutability(List<Node> localNodes, List<Node> globalNodes) {
        List<String> nodeVariables = getNodeVariables(localNodes);

        Set<String> globalKeys = new HashSet<>(getNodeVariables(globalNodes));

        return nodeVariables.stream()
            .filter(globalKeys::contains)
            .findFirst().orElse(null);
    }

    public static void validateVariables(
        Node node,
        List<Node> body,
        List<String> variables,
        Set<String> fileScopedVariables,
        Set<String> globalScopedVariables) {
        // duplicate inline variable
        String duplicate = ScopingPolice.validateLocalImmutabilityVariables(variables);

        if (duplicate != null) {
            throw new Defer(node, String.format("duplicate inline variable '%s'!", duplicate));
        }

        validateGlobalAndFileScope(node, body, variables, fileScopedVariables, globalScopedVariables, false);
    }

    public static void validateBody(
        Node node,
        List<Node> body,
        List<String> variables,
        Set<String> fileScopedVariables,
        Set<String> globalScopedVariables) {
        // duplicate body variable
        String duplicate = ScopingPolice.validateLocalImmutability(body);

        if (duplicate != null) {
            throw new Defer(
                ScopingPolice.getKeyNode(body, duplicate),
                String.format("duplicate for body variable '%s'!", duplicate));
        }

        List<String> bodyVariables = ScopingPolice.getNodeVariables(body);

        // inline scoped variable defined as body variable
        duplicate = bodyVariables.stream().filter(variables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(
                ScopingPolice.getKeyNode(body, duplicate),
                String.format("'%s' is already defined inline and cannot be reused!", duplicate));
        }

        validateGlobalAndFileScope(node, body, bodyVariables, fileScopedVariables, globalScopedVariables, true);
    }

    public static void validateGlobalAndFileScope(
        Node node,
        List<Node> body,
        List<String> variables,
        Set<String> fileScopedVariables,
        Set<String> globalScopedVariables,
        boolean isBody) {

        // file scoped variable defined as inline/body variable
        String duplicate = variables.stream().filter(fileScopedVariables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(
                isBody ? ScopingPolice.getKeyNode(body, duplicate) : node,
                String.format("'%s' is already defined in the file scope and cannot be reused!", duplicate));
        }

        // global scoped variable defined as inline/body variable
        duplicate = variables.stream().filter(globalScopedVariables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(
                isBody ? ScopingPolice.getKeyNode(body, duplicate) : node,
                String.format("'%s' is already defined in the global scope and cannot be reused!", duplicate));
        }
    }
}
