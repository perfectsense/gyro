package beam.core.diff;

import beam.core.BeamUI;

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
    public void writeTo(BeamUI ui) {
        ui.write(diffable.toDisplayString());
    }

    @Override
    protected void doExecute() {
    }

}
