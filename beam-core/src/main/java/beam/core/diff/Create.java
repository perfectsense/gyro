package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Create extends Change {

    private final Diffable diffable;

    public Create(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    @Override
    public void writeTo(BeamUI ui) {
        ui.write("@|green + Create %s|@", diffable.toDisplayString());
    }

    @Override
    protected void doExecute() {
        if (diffable instanceof Resource) {
            ((Resource) diffable).create();
        }
    }

}
