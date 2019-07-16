package gyro.core.diff;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

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
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) throws Exception {
        Resource current = (Resource) currentDiffable;
        Resource pending = (Resource) pendingDiffable;

        state.update(this);

        for (ChangeProcessor processor : processors) {
            processor.beforeUpdate(ui, state, current, pending, changedFields);
        }

        if (!state.isTest()) {
            pending.update(
                ui,
                state,
                current,
                changedFields.stream()
                    .map(DiffableField::getName)
                    .collect(Collectors.toSet()));
        }

        for (ChangeProcessor processor : processors) {
            processor.afterUpdate(ui, state, current, pending, changedFields);
        }

        return ExecutionResult.OK;
    }

}
