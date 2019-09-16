package gyro.core.diff;

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.resource.DiffableField;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public abstract class ChangeProcessor {

    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
    }

    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
    }

    public void beforeUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
    }

    public void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
    }

    public void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
    }

    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
    }

}
