package gyro.core.diff;

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.resource.DiffableField;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public interface ChangeProcessor {

    default void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
    }

    default void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
    }

    default void beforeUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
    }

    default void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
    }

    default void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
    }

    default void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
    }

}
