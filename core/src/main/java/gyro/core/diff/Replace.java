package gyro.core.diff;

import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.BeamUI;
import gyro.lang.Resource;
import gyro.lang.Workflow;
import gyro.lang.ast.scope.State;

public class Replace extends Change {

    private final Diffable currentDiffable;
    private final Diffable pendingDiffable;
    private final Set<DiffableField> changedFields;
    private final Workflow workflow;

    public Replace(Diffable currentDiffable, Diffable pendingDiffable, Set<DiffableField> changedFields) {
        this.currentDiffable = currentDiffable;
        this.pendingDiffable = pendingDiffable;
        this.changedFields = changedFields;

        Resource pendingResource = (Resource) pendingDiffable;

        this.workflow = pendingResource.scope()
                .getRootScope()
                .getWorkflows()
                .stream()
                .filter(w -> w.getForType().equals(pendingResource.resourceType()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    private void writeFields(BeamUI ui) {
        if (!ui.isVerbose()) {
            return;
        }

        for (DiffableField field : DiffableType.getInstance(pendingDiffable.getClass()).getFields()) {
            if (!Diffable.class.isAssignableFrom(field.getItemClass())) {
                if (changedFields.contains(field)) {
                    writeDifference(ui, field, currentDiffable, pendingDiffable);

                } else {
                    ui.write("\n· %s: %s",
                            field.getBeamName(),
                            stringify(field.getValue(pendingDiffable)));
                }
            }
        }
    }

    @Override
    public void writePlan(BeamUI ui) {
        ui.write("@|cyan ⇅ Replace %s|@", currentDiffable.toDisplayString());
        ui.write(" (because of %s, ", changedFields.stream()
                .filter(f -> !f.isUpdatable())
                .map(DiffableField::getBeamName)
                .collect(Collectors.joining(", ")));

        if (workflow != null) {
            ui.write("using %s", workflow.getName());

        } else {
            ui.write("skipping without a workflow");
        }

        ui.write(")");
        writeFields(ui);
    }

    @Override
    public void writeExecution(BeamUI ui) {
        ui.write("@|magenta ⇅ Replacing %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public boolean execute(BeamUI ui, State state) throws Exception {
        if (workflow == null) {
            return false;
        }

        if (ui.isVerbose()) {
            ui.write("\n");
        }

        ui.write("\n@|magenta ~ Executing %s workflow|@", workflow.getName());
        workflow.execute(ui, state, (Resource) pendingDiffable);
        return true;
    }

}
