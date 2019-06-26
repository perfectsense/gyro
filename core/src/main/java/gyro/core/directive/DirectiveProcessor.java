package gyro.core.directive;

import java.util.List;
import java.util.stream.Collectors;

import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public abstract class DirectiveProcessor {

    public static List<Object> resolveArguments(Scope scope, DirectiveNode node) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        return node.getArguments()
            .stream()
            .map(a -> evaluator.visit(a, scope))
            .collect(Collectors.toList());
    }

    public static Scope resolveBody(Scope scope, DirectiveNode node) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Scope bodyScope = new Scope(scope);

        evaluator.visitBody(node.getBody(), bodyScope);
        return bodyScope;
    }

    public abstract String getName();

    public abstract void process(Scope scope, DirectiveNode node) throws Exception;

}
