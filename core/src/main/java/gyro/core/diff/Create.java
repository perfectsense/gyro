package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.lang.Resource;
import gyro.lang.ast.scope.State;

public class Create extends Change {

    private final Diffable diffable;

    public Create(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    private void writeFields(GyroUI ui) {
        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!field.shouldBeDiffed()) {
                ui.write("\n· %s: %s", field.getBeamName(), stringify(field.getValue(diffable)));
            }
        }
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|green + Create %s|@", diffable.toDisplayString());

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta + Creating %s|@", diffable.toDisplayString());

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public boolean execute(GyroUI ui, State state) {
        Resource resource = (Resource) diffable;

        if (state.isTest()) {
            resource.testCreate();

        } else {
            resource.create();
        }

        return true;
    }

}
