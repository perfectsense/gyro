package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

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
    public boolean execute(GyroUI ui, State state) {
        if (!state.isTest()) {
            ((Resource) diffable).delete();
        }

        return true;
    }

}
