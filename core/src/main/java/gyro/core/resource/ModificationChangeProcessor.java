package gyro.core.resource;

import gyro.core.GyroUI;
import gyro.core.diff.ChangeProcessor;
import gyro.core.scope.State;

import java.util.Set;

public class ModificationChangeProcessor extends ChangeProcessor {

    @Override
    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.beforeCreate(ui, state, resource);
        }
    }

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.afterCreate(ui, state, resource);
        }
    }

    @Override
    public void beforeUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current)) {
            modification.beforeUpdate(ui, state, current, pending, changedFields);
        }
    }

    @Override
    public void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current)) {
            modification.afterUpdate(ui, state, current, pending, changedFields);
        }
    }

    @Override
    public void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.beforeDelete(ui, state, resource);
        }
    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.afterDelete(ui, state, resource);
        }
    }

}
