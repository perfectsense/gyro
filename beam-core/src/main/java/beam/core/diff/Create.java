package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;
import beam.lang.ast.scope.State;

public class Create extends Change {

    private final Diffable diffable;

    public Create(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    private void writeFields(BeamUI ui) {
        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!Diffable.class.isAssignableFrom(field.getItemClass())) {
                ui.write("\n· %s: %s", field.getBeamName(), stringify(field.getValue(diffable)));
            }
        }
    }

    @Override
    public void writePlan(BeamUI ui) {
        ui.write("@|green + Create %s|@", diffable.toDisplayString());

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public void writeExecution(BeamUI ui) {
        ui.write("@|magenta + Creating %s|@", diffable.toDisplayString());

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public void execute(BeamUI ui, State state) {
        Resource resource = (Resource) diffable;

        if (state.isTest()) {
            resource.testCreate();

        } else {
            resource.create();
        }
    }

}
