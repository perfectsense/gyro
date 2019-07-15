package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

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
                ui.write("\n· %s: %s", field.getName(), stringify(field.getValue(diffable)));
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
    public ExecutionResult execute(GyroUI ui, State state) {
        Resource resource = (Resource) diffable;

        state.update(this);

        if (state.isTest()) {
            resource.testCreate(ui, state);

        } else {
            resource.create(ui, state);
        }

        return ExecutionResult.OK;
    }

}
