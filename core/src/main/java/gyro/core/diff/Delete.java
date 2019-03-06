package gyro.core.diff;

import gyro.core.BeamUI;
import gyro.lang.Resource;
import gyro.lang.ast.scope.State;

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
    public void writePlan(BeamUI ui) {
        ui.write("@|red - Delete %s|@", diffable.toDisplayString());
    }

    @Override
    public void writeExecution(BeamUI ui) {
        ui.write("@|magenta - Deleting %s|@", diffable.toDisplayString());
    }

    @Override
    public boolean execute(BeamUI ui, State state) {
        if (!state.isTest()) {
            ((Resource) diffable).delete();
        }

        return true;
    }

}
