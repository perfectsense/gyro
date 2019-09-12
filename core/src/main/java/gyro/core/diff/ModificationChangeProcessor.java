package gyro.core.diff;

import gyro.core.GyroUI;
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

public class ModificationChangeProcessor extends ChangeProcessor {

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource).values()) {
            modification.create(ui, state);
        }
    }

    @Override
    public void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current).values()) {
            modification.update(ui, state, current, changedFields);
        }
    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource).values()) {
            modification.delete(ui, state);
        }
    }

}
