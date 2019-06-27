package gyro.core.workflow;

import gyro.core.GyroUI;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;

public abstract class Action {

    public abstract void execute(GyroUI ui, State state, RootScope pending, Scope scope);

}
