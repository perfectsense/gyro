package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.State;

public class Keep extends Change {

    private final Diffable diffable;

    public Keep(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("= Keep %s", diffable.toDisplayString());
    }

    @Override
    public void writeExecution(GyroUI ui) {
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state) {
        state.update(this);
        return null;
    }

}
