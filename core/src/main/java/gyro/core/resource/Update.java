package gyro.core.resource;

import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.GyroUI;

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

    private void writeFields(GyroUI ui) {
        if (ui.isVerbose()) {
            for (DiffableField field : changedFields) {
                writeDifference(ui, field, currentDiffable, pendingDiffable);
            }

        } else {
            ui.write(" (change %s)", changedFields.stream()
                .map(DiffableField::getName)
                .collect(Collectors.joining(", ")));
        }
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|yellow ⟳ Update %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta ⟳ Updating %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public boolean execute(GyroUI ui, State state) throws Exception {
        if (!state.isTest()) {
            ((Resource) pendingDiffable).update(
                (Resource) currentDiffable,
                changedFields.stream()
                    .map(DiffableField::getName)
                    .collect(Collectors.toSet()));

            state.update(this);
        }

        return true;
    }

}
