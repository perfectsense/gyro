package gyro.core.diff;

import java.util.List;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public class Delete extends Change {

    private final Diffable diffable;

    public Delete(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|red - Delete %s|@", getLabel(diffable, false));
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta - Deleting %s|@", getLabel(diffable, true));
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) throws Exception {
        Resource resource = (Resource) diffable;

        state.update(this);

        for (ChangeProcessor processor : processors) {
            processor.beforeDelete(ui, state, resource);
        }

        if (!state.isTest()) {
            resource.delete(ui, state);
        }

        for (ChangeProcessor processor : processors) {
            processor.afterDelete(ui, state, resource);
        }

        return ExecutionResult.OK;
    }

}
