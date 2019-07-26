package gyro.core.resource;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.core.workflow.WaitAction;
import gyro.core.workflow.WorkflowSettings;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WaitForDirective extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "wait-for";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        List<Node> atMostArguments = validateOptionArguments(node, "at-most", 0, 1);
        List<Node> checkEveryArguments = validateOptionArguments(node, "check-every", 0, 1);
        List<Node> timeUnitArguments = validateOptionArguments(node, "time-unit", 0, 1);

        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        long atMost = atMostArguments.isEmpty() ? 60 : (Integer) evaluator.visit(atMostArguments.get(0), scope);
        long checkEvery = checkEveryArguments.isEmpty() ? 10 : (Integer) evaluator.visit(checkEveryArguments.get(0), scope);
        TimeUnit timeUnit = timeUnitArguments.isEmpty() ? TimeUnit.SECONDS : TimeUnit.valueOf((String) evaluator.visit(timeUnitArguments.get(0), scope));

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new WaitAction(node.getArguments().get(0), atMost, checkEvery, timeUnit));
    }

}
