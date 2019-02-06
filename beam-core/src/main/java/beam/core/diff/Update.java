package beam.core.diff;

import java.util.Set;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Update extends Change {

    private final Diffable currentDiffable;
    private final Diffable pendingDiffable;
    private final Set<String> changedProperties;
    private final String changedDisplay;

    public Update(Diffable currentDiffable, Diffable pendingDiffable, Set<String> changedProperties, String changedDisplay) {
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
        String s = "Update " + currentDiffable.toDisplayString();

        if (changedDisplay.length() > 0) {
            s += " (";
            s += changedDisplay;
            s += ")";
        }

        if (s.contains("@|")) {
            ui.write(" * %s", s);

        } else {
            ui.write("@|yellow * %s|@", s);
        }
    }

    @Override
    protected void doExecute() {
        if (currentDiffable instanceof Resource && pendingDiffable instanceof Resource) {
            ((Resource) pendingDiffable).update((Resource) currentDiffable, changedProperties);
        }
    }

}
