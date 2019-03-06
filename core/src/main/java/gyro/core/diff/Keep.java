package gyro.core.diff;

import gyro.core.BeamUI;
import gyro.lang.ast.scope.State;

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
    public void writePlan(BeamUI ui) {
        ui.write("= Keep %s", diffable.toDisplayString());
    }

    @Override
    public void writeExecution(BeamUI ui) {
        ui.write("= Keeping %s", diffable.toDisplayString());
    }

    @Override
    public boolean execute(BeamUI ui, State state) {
        return true;
    }

}
