package gyro.core.workflow;

import gyro.core.GyroUI;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;

public abstract class Action {

    public abstract void execute(GyroUI ui, State state, RootScope pending, Scope scope);

}
