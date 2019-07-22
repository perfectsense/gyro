package gyro.core.diff;

import java.util.List;

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
        ui.write("@|green + Create %s|@", getLabel(diffable, false));

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta + Creating %s|@", getLabel(diffable, true));

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) throws Exception {
        Resource resource = (Resource) diffable;

        state.update(this);

        for (ChangeProcessor processor : processors) {
            processor.beforeCreate(ui, state, resource);
        }

        if (state.isTest()) {
            resource.testCreate(ui, state);

        } else {
            resource.create(ui, state);
        }

        for (ChangeProcessor processor : processors) {
            processor.afterCreate(ui, state, resource);
        }

        return ExecutionResult.OK;
    }

}
