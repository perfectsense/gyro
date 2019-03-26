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

    @Override
    protected boolean isCompleted() {
        boolean completed = true;
        if (getDiffable() instanceof Resource) {
            Resource resource = (Resource) getDiffable();
            completed = changed.get() && resource.isDeleted();
        }

        return completed;
    }

    @Override
    public boolean isReady() {
        boolean ready = true;
        for (Diffable diffable : getDiffable().dependents()) {
            ready = ready && diffable.change().isCompleted();
        }

        return ready;
    }
}
