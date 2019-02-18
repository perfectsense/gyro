package beam.core.diff;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import beam.core.BeamUI;
import beam.lang.Resource;
import beam.lang.Workflow;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.State;

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
                .filter(w -> w.isTriggerable(pendingResource, changedFields))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    private void writeFields(BeamUI ui) {
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
        ui.write("@|cyan ⤢ Replace %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public void writeExecution(BeamUI ui) {
        ui.write("@|magenta ⤢ Replacing %s|@", currentDiffable.toDisplayString());
        writeFields(ui);
    }

    @Override
    public void execute(BeamUI ui, State state) throws Exception {
        if (workflow == null) {
            return;
        }

        if (ui.isVerbose()) {
            ui.write("\n");
        }

        ui.write("\n@|magenta ~ Executing %s workflow|@", workflow.getName());

        Resource currentResource = (Resource) currentDiffable;
        Resource pendingResource = (Resource) pendingDiffable;
        Map<String, Object> pendingValues = new LinkedHashMap<>();

        pendingValues.put("_current", pendingResource.resourceIdentifier());

        for (DiffableField field : changedFields) {
            if (!field.isUpdatable()) {
                pendingValues.put(field.getBeamName(), field.getValue(pendingResource));
                field.setValue(pendingResource, field.getValue(currentResource));
            }
        }

        workflow.execute(ui, state, pendingValues);
    }

}
