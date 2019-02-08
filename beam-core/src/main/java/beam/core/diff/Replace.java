package beam.core.diff;

import java.util.Set;

import beam.core.BeamUI;

public class Replace extends Change {

    private final Diffable currentDiffable;
    private final Diffable pendingDiffable;
    private final Set<String> changedProperties;
    private final String changedDisplay;

    public Replace(Diffable currentDiffable, Diffable pendingDiffable, Set<String> changedProperties, String changedDisplay) {
        this.currentDiffable = currentDiffable;
        this.pendingDiffable = pendingDiffable;
        this.changedProperties = changedProperties;
        this.changedDisplay = changedDisplay;
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    @Override
    public void writeTo(BeamUI ui) {
        ui.write(
                "@|blue * Replace %s (%s)|@",
                currentDiffable.toDisplayString(),
                changedDisplay);
    }

    @Override
    protected void doExecute() {
    }

}
