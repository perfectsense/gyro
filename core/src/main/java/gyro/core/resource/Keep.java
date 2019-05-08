package gyro.core.resource;

import gyro.core.GyroUI;

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
        ui.write("= Keeping %s", diffable.toDisplayString());
    }

    @Override
    public boolean execute(GyroUI ui, State state) {
        return true;
    }

}
