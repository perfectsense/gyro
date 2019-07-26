package gyro.core.diff;

import gyro.core.resource.DiffableInternals;
import gyro.core.scope.State;

public class Context {

    private final Change change;
    private final State state;

    public Context(Change change, State state) {
        this.change = change;
        this.state = state;
    }

    public void save() {
        DiffableInternals.refresh(change.getDiffable());
        state.save();
    }

}
