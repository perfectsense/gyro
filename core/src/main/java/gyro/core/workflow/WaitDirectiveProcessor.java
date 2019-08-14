package gyro.core.workflow;

import gyro.core.Waiter;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WaitDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "wait";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        validateOptionArguments(node, "unit", 0, 1);
        validateOptionArguments(node, "at-most", 0, 1);
        validateOptionArguments(node, "check-every", 0, 1);

        Waiter waiter = new Waiter();

        TimeUnit unit = Optional.ofNullable(getOptionArgument(scope, node, "unit", TimeUnit.class, 0))
            .orElse(TimeUnit.SECONDS);

        Optional.ofNullable(getOptionArgument(scope, node, "at-most", Long.class, 0))
            .ifPresent(d -> waiter.atMost(d, unit));

        Optional.ofNullable(getOptionArgument(scope, node, "check-every", Long.class, 0))
            .ifPresent(d -> waiter.checkEvery(d, unit));

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new WaitAction(waiter, node.getArguments().get(0)));
    }

}
