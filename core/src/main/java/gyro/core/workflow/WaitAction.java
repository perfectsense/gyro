package gyro.core.workflow;

import gyro.core.GyroCore;
import gyro.core.GyroUI;
import gyro.core.Wait;
import gyro.core.resource.DiffableInternals;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

import java.util.concurrent.TimeUnit;

public class WaitAction extends Action {

    private final Node condition;
    private final long atMost;
    private final long checkEvery;
    private final TimeUnit timeUnit;

    public WaitAction(Node condition, long atMost, long checkEvery, TimeUnit timeUnit) {
        this.condition = condition;
        this.atMost = atMost;
        this.checkEvery = checkEvery;
        this.timeUnit = timeUnit;
    }

    @Override
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        Wait.atMost(atMost, timeUnit)
            .checkEvery(checkEvery, timeUnit)
            .until(() -> {
                scope.getRootScope().findResources().forEach(DiffableInternals::refresh);
                GyroCore.ui().write(String.format("Waiting for %s ...%n", condition));

                return Boolean.TRUE.equals(scope.getRootScope().getEvaluator().visit(condition, scope));
            });
    }

}
