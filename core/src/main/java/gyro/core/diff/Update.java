package gyro.core.diff;

import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.BeamUI;
import gyro.lang.Resource;
import gyro.lang.ast.scope.State;

public class Update extends Change {

    private final Diffable currentDiffable;
    private final Diffable pendingDiffable;
    private final Set<DiffableField> changedFields;

    public Update(Diffable currentDiffable, Diffable pendingDiffable, Set<DiffableField> changedFields) {
        this.currentDiffable = currentDiffable;
        this.pendingDiffable = pendingDiffable;
        this.changedFields = changedFields;
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    private void writeFields(BeamUI ui) {
        if (ui.isVerbose()) {
            for (DiffableField field : changedFields) {
                writeDifference(ui, field, currentDiffable, pendingDiffable);
            }

        } else {
            ui.write(" (change %s)", changedFields.stream()
                    .map(DiffableField::getBeamName)
                    .collect(Collectors.joining(", ")));
        }
    }

    @Override
    public void writePlan(BeamUI ui) {
        ui.write("@|yellow ⟳ Update %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public void writeExecution(BeamUI ui) {
        ui.write("@|magenta ⟳ Updating %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public boolean execute(BeamUI ui, State state) {
        if (!state.isTest()) {
            ((Resource) pendingDiffable).update(
                    (Resource) currentDiffable,
                    changedFields.stream()
                            .map(DiffableField::getBeamName)
                            .collect(Collectors.toSet()));
        }

        return true;
    }

    @Override
    protected boolean isCompleted() {
        boolean completed = true;
        if (getDiffable() instanceof Resource) {
            Resource resource = (Resource) getDiffable();
            completed = changed.get() && resource.isUpdated();
        }

        return completed;
    }
}
