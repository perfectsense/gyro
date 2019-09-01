package gyro.core.diff;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.ModificationField;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<Diffable, ModificationField> fieldByDiffable = DiffableInternals.getModificationByField(pending)
            .entrySet()
            .stream()
            .filter(e -> changedFields.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        for (Map.Entry<Diffable, ModificationField> entry : fieldByDiffable.entrySet()) {
            if (entry.getKey() instanceof Resource) {
                Resource currentModification = (Resource) DiffableInternals.getModificationByField(current).get(entry.getValue());
                ((Resource) entry.getKey()).update(ui, state, currentModification, changedFields
                    .stream()
                    .map(DiffableField::getName)
                    .collect(Collectors.toSet())
                );
            }
        }
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
