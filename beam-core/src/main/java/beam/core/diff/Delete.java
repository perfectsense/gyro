package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;
import beam.lang.ast.scope.State;

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
        ((Resource) diffable).delete();
        return true;
    }

}
