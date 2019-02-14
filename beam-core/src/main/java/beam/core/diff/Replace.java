package beam.core.diff;

import java.util.Set;
import java.util.stream.Collectors;

import beam.core.BeamUI;

public class Replace extends Change {

    private final Diffable currentDiffable;
    private final Diffable pendingDiffable;
    private final Set<DiffableField> changedFields;

    public Replace(Diffable currentDiffable, Diffable pendingDiffable, Set<DiffableField> changedFields) {
        this.currentDiffable = currentDiffable;
        this.pendingDiffable = pendingDiffable;
        this.changedFields = changedFields;
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    private void writeFields(BeamUI ui) {
        ui.write(" (because of %s)", changedFields.stream()
                .filter(f -> !f.isUpdatable())
                .map(DiffableField::getBeamName)
                .collect(Collectors.joining(", ")));

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
                            field.getValue(pendingDiffable));
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
    public void execute() {
        throw new UnsupportedOperationException();
    }

}
