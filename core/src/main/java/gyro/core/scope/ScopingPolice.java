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

    private enum SCOPETYPE {
        LOCAL,
        GLOBAL,
        INLINE;

        @Override
        public String toString() {
            switch (this) {
                case LOCAL: return "local";
                case GLOBAL: return "global";
                case INLINE: return "inline";
                default: return super.toString();
            }
        }
    }

    public static List<String> getNodeVariables(List<Node> nodes) {
        return nodes.stream()
            .filter(o -> o instanceof PairNode)
            .map(o -> (String) ((ValueNode) ((PairNode) o).getKey()).getValue())
            .collect(Collectors.toList());
    }

    public static List<Node> getKeyNodes(List<Node> nodes, String key) {
        return nodes.stream()
            .filter(o -> o instanceof PairNode)
            .filter(o -> ((ValueNode) ((PairNode) o).getKey()).getValue().equals(
                key)).collect(Collectors.toList());
    }

    public static Node getKeyNode(List<Node> nodes, String key) {
        return getKeyNode(nodes, key, 0);
    }

    public static Node getKeyNode(List<Node> nodes, String key, int index) {
        List<Node> keyNodes = getKeyNodes(nodes, key);
        return keyNodes.size() > index ? keyNodes.get(index) : null;
    }

    public static String validateLocalImmutabilityVariables(List<String> nodeVariables) {
        return nodeVariables.stream()
            .filter(e -> Collections.frequency(nodeVariables, e) > 1)
            .findFirst().orElse(null);
    }

    public static void validateLocalImmutability(List<Node> nodes) {
        String duplicate = validateLocalImmutabilityVariables(getNodeVariables(nodes));

        if (duplicate != null) {
            throw new Defer(
                ScopingPolice.getKeyNode(nodes, duplicate, 1),
                getMessage(SCOPETYPE.LOCAL, duplicate));
        }
    }

    public static void validateGlobalImmutability(List<Node> localNodes, List<Node> globalNodes) {
        List<String> nodeVariables = getNodeVariables(localNodes);

        Set<String> globalKeys = new HashSet<>(getNodeVariables(globalNodes));

        String duplicate = nodeVariables.stream()
            .filter(globalKeys::contains)
            .findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(
                ScopingPolice.getKeyNode(localNodes, duplicate),
                getMessage(SCOPETYPE.GLOBAL, duplicate));
        }
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
            throw new Defer(node, getMessage(SCOPETYPE.INLINE, duplicate));
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
        ScopingPolice.validateLocalImmutability(body);

        List<String> bodyVariables = ScopingPolice.getNodeVariables(body);

        // inline scoped variable defined as body variable
        String duplicate = bodyVariables.stream().filter(variables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(
                ScopingPolice.getKeyNode(body, duplicate),
                getMessage(SCOPETYPE.INLINE, duplicate));
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
                getMessage(SCOPETYPE.LOCAL, duplicate));
        }

        // global scoped variable defined as inline/body variable
        duplicate = variables.stream().filter(globalScopedVariables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(
                isBody ? ScopingPolice.getKeyNode(body, duplicate) : node,
                getMessage(SCOPETYPE.GLOBAL, duplicate));
        }
    }

    private static String getMessage(SCOPETYPE scopeType, String variable) {
        return String.format("'%s' is already defined as a @|bold %s|@ variable and cannot be reused!", variable, scopeType);
    }
}
