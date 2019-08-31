package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

import java.util.HashSet;
import java.util.Set;

public class ModificationProcessor extends ChangeProcessor {

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        Set<Diffable> modifications = new HashSet<>(DiffableInternals.getModificationByField(resource).values());
        for (Diffable modification : modifications) {
            if (modification instanceof Resource) {
                ((Resource) modification).create(ui, state);
            }
        }
    }

    @Override
    public void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {

    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
        Set<Diffable> modifications = new HashSet<>(DiffableInternals.getModificationByField(resource).values());
        for (Diffable modification : modifications) {
            if (modification instanceof Resource) {
                ((Resource) modification).delete(ui, state);
            }
        }
    }

}
