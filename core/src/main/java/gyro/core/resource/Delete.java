package gyro.core.resource;

import gyro.core.GyroUI;

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
    public boolean execute(GyroUI ui, State state) throws Exception {
        if (!state.isTest()) {
            ((Resource) diffable).delete();
            state.update(this);

            ((Resource) diffable).afterDelete();
            state.update(this);
        }

        return true;
    }

}
