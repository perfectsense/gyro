package gyro.core.workflow;

import gyro.core.GyroUI;
import gyro.core.Waiter;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class WaitAction extends Action {

    private final Waiter waiter;
    private final Node condition;

    public WaitAction(Waiter waiter, Node condition) {
        this.waiter = waiter;
        this.condition = condition;
    }

    @Override
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        ui.write("@|magenta ⧖ Waiting for: %s|@\n", condition);

        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        ui.indented(() ->
            waiter.until(() -> {
                ui.write("@|magenta ✓ Checking |@");

                boolean result = Boolean.TRUE.equals(evaluator.visit(condition, scope));

                ui.write(result ? "@|green PASSED|@" : "@|red FAILED|@");
                ui.write("\n");
                return result;
            })
        );
    }

}
