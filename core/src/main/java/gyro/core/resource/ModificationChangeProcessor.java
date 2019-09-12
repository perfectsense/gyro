package gyro.core.resource;

import gyro.core.GyroUI;
import gyro.core.diff.ChangeProcessor;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Modification;
import gyro.core.resource.ModificationField;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModificationChangeProcessor implements ChangeProcessor {

    @Override
    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource).values()) {
            modification.beforeCreate(ui, state, resource);
        }
    }

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource).values()) {
            modification.afterCreate(ui, state, resource);
        }
    }

    @Override
    public void beforeUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current).values()) {
            modification.afterUpdate(ui, state, current, pending, changedFields);
        }
    }

    @Override
    public void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current).values()) {
            modification.afterUpdate(ui, state, current, pending, changedFields);
        }
    }

    @Override
    public void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource).values()) {
            modification.afterDelete(ui, state, resource);
        }
    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource).values()) {
            modification.afterDelete(ui, state, resource);
        }
    }

}
