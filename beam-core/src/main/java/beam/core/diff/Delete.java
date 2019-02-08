package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;

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
    public void writeTo(BeamUI ui) {
        ui.write("@|red - Delete %s|@", diffable.toDisplayString());
    }

    @Override
    protected void doExecute() {
        if (diffable instanceof Resource) {
            ((Resource) diffable).delete();
        }
    }

}
