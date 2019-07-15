package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.Resource;
import gyro.core.resource.State;

public class Delete extends Change {

    private final Diffable diffable;

    public Delete(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|red - Delete %s|@", diffable.toDisplayString());
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta - Deleting %s|@", diffable.toDisplayString());
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state) {
        if (state.isTest()) {
            state.update(this);

        } else {
            Resource resource = (Resource) diffable;

            resource.delete();
            state.update(this);
            resource.afterDelete();
            state.update(this);
        }

        return ExecutionResult.OK;
    }

}
