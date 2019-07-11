package gyro.core.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroUI;

public class Diff {

    private final List<Diffable> currentDiffables;
    private final List<Diffable> pendingDiffables;
    private final List<Change> changes = new ArrayList<>();

    public Diff(Collection<? extends Diffable> currentDiffables, Collection<? extends Diffable> pendingDiffables) {
        this.currentDiffables = currentDiffables != null
            ? new ArrayList<>(currentDiffables)
            : Collections.emptyList();

        this.pendingDiffables = pendingDiffables != null
            ? new ArrayList<>(pendingDiffables)
            : Collections.emptyList();
    }

    public Diff(Diffable currentDiffable, Diffable pendingDiffable) {
        this.currentDiffables = currentDiffable != null
            ? Collections.singletonList(currentDiffable)
            : Collections.emptyList();

        this.pendingDiffables = pendingDiffable != null
            ? Collections.singletonList(pendingDiffable)
            : Collections.emptyList();
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void diff() {
        Map<String, Diffable> currentDiffables = this.currentDiffables.stream().collect(
            LinkedHashMap::new,
            (map, r) -> map.put(r.primaryKey(), r),
            Map::putAll
        );

        for (Diffable pendingDiffable : pendingDiffables) {
            resolve(pendingDiffable);

            Diffable currentDiffable = currentDiffables.remove(pendingDiffable.primaryKey());

            changes.add(currentDiffable == null
                ? newCreate(pendingDiffable)
                : newUpdate(currentDiffable, pendingDiffable));
        }

        for (Diffable resource : currentDiffables.values()) {
            changes.add(newDelete(resource));
        }
    }

    @SuppressWarnings("unchecked")
    private Change newCreate(Diffable diffable) {
        Create create = new Create(diffable);

        diffable.change = create;

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!field.shouldBeDiffed()) {
                continue;
            }

            Object value = field.getValue(diffable);
            Diff diff;

            if (value instanceof Collection) {
                diff = new Diff(null, (Collection<Diffable>) value);

            } else if (value != null) {
                diff = new Diff(null, (Diffable) value);

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                create.getDiffs().add(diff);
            }
        }

        return create;
    }

    @SuppressWarnings("unchecked")
    private Change newUpdate(Diffable currentDiffable, Diffable pendingDiffable) {
        List<Diff> diffs = new ArrayList<>();
        DiffableType<? extends Diffable> type = DiffableType.getInstance(currentDiffable.getClass());

        for (DiffableField field : type.getFields()) {
            if (!field.shouldBeDiffed()) {
                continue;
            }

            Object currentValue = field.getValue(currentDiffable);
            Object pendingValue = field.getValue(pendingDiffable);
            Diff diff;

            if (pendingValue instanceof Collection) {
                diff = new Diff((Collection<Diffable>) currentValue, (Collection<Diffable>) pendingValue);

            } else if (currentValue != null) {
                if (pendingValue != null) {
                    diff = new Diff((Diffable) currentValue, (Diffable) pendingValue);

                } else {
                    diff = new Diff((Diffable) currentValue, null);
                }

            } else if (pendingValue != null) {
                diff = new Diff(null, (Diffable) pendingValue);

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                diffs.add(diff);
            }
        }

        Set<DiffableField> changedFields = diffFields(currentDiffable, pendingDiffable);

        diffs.stream()
            .map(Diff::getChanges)
            .flatMap(List::stream)
            .filter(c -> !(c instanceof Keep))
            .map(Change::getDiffable)
            .filter(d -> !(d instanceof Resource))
            .map(Diffable::name)
            .map(type::getField)
            .forEach(changedFields::add);

        Change change;

        if (changedFields.isEmpty()) {
            change = new Keep(pendingDiffable);

        } else if (changedFields.stream().allMatch(DiffableField::isUpdatable)) {
            change = new Update(currentDiffable, pendingDiffable, changedFields);

        } else {
            change = new Replace(currentDiffable, pendingDiffable, changedFields);
        }

        currentDiffable.change = change;
        pendingDiffable.change = change;
        change.getDiffs().addAll(diffs);

        return change;
    }

    private Set<DiffableField> diffFields(Diffable currentDiffable, Diffable pendingDiffable) {
        Set<String> currentConfiguredFields = currentDiffable.configuredFields != null
            ? currentDiffable.configuredFields
            : ImmutableSet.of();

        Set<DiffableField> changedFields = new LinkedHashSet<>();

        for (DiffableField field : DiffableType.getInstance(currentDiffable.getClass()).getFields()) {

            // Skip nested diffables since they're handled by the diff system.
            if (field.shouldBeDiffed()) {
                continue;
            }

            Object currentValue = field.getValue(currentDiffable);
            Object pendingValue = field.getValue(pendingDiffable);

            // Skip if the value didn't change.
            if (Objects.equals(currentValue, pendingValue)) {
                continue;
            }

            String key = field.getName();

            // Skip if there isn't a pending value and the field wasn't
            // previously configured. This means that a field was
            // automatically populated in code so we should keep it as is.
            if (pendingValue == null && !currentConfiguredFields.contains(key)) {
                continue;
            }

            changedFields.add(field);
        }

        return changedFields;
    }

    @SuppressWarnings("unchecked")
    private Change newDelete(Diffable diffable) {
        Delete delete = new Delete(diffable);

        diffable.change = delete;

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!field.shouldBeDiffed()) {
                continue;
            }

            Object value = field.getValue(diffable);
            Diff diff;

            if (value instanceof Collection) {
                diff = new Diff((Collection<Diffable>) value, null);

            } else if (value != null) {
                diff = new Diff((Diffable) value, null);

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                delete.getDiffs().add(diff);
            }
        }

        return delete;
    }

    public boolean hasChanges() {
        List<Change> changes = getChanges();

        for (Change change : changes) {
            if (!(change instanceof Keep)) {
                return true;
            }
        }

        for (Change change : changes) {
            for (Diff diff : change.getDiffs()) {
                if (diff.hasChanges()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean write(GyroUI ui) {
        if (!hasChanges()) {
            return false;
        }

        boolean written = false;

        for (Change change : getChanges()) {
            List<Diff> changeDiffs = change.getDiffs();

            if (change instanceof Keep) {
                boolean hasChanges = false;

                for (Diff changeDiff : changeDiffs) {
                    if (changeDiff.hasChanges()) {
                        hasChanges = true;
                        break;
                    }
                }

                if (!hasChanges) {
                    continue;
                }
            }

            written = true;

            if (!change.getDiffable().writePlan(ui, change)) {
                change.writePlan(ui);
            }

            ui.write(ui.isVerbose() ? "\n\n" : "\n");

            ui.indented(() -> {
                for (Diff d : change.getDiffs()) {
                    d.write(ui);
                }
            });
        }

        return written;
    }

    public void executeCreateOrUpdate(GyroUI ui, State state) {
        for (Change change : getChanges()) {
            if (change instanceof Create || change instanceof Update) {
                execute(ui, state, change);
            }

            for (Diff d : change.getDiffs()) {
                d.executeCreateOrUpdate(ui, state);
            }
        }
    }

    public void executeReplace(GyroUI ui, State state) {
        for (Change change : getChanges()) {
            if (change instanceof Replace) {
                execute(ui, state, change);
            }

            for (Diff d : change.getDiffs()) {
                d.executeReplace(ui, state);
            }
        }
    }

    public void executeDelete(GyroUI ui, State state) {
        for (ListIterator<Change> j = getChanges().listIterator(getChanges().size()); j.hasPrevious(); ) {
            Change change = j.previous();

            for (Diff d : change.getDiffs()) {
                d.executeDelete(ui, state);
            }

            if (change instanceof Delete) {
                execute(ui, state, change);
            }
        }
    }

    private void execute(GyroUI ui, State state, Change change) {
        Diffable diffable = change.getDiffable();

        if (!(diffable instanceof Resource)) {
            return;
        }

        if (change.changed.compareAndSet(false, true)) {
            resolve(diffable);

            if (!diffable.writeExecution(ui, change)) {
                change.writeExecution(ui);
            }

            if (change.execute(ui, state)) {
                ui.write(ui.isVerbose() ? "\n@|bold,green OK|@\n\n" : " @|bold,green OK|@\n");

            } else {
                ui.write(ui.isVerbose() ? "\n@|bold,yellow SKIPPED|@\n\n" : " @|bold,yellow SKIPPED|@\n");
            }
        }
    }

    private void resolve(Object object) {
        if (object instanceof Diffable) {
            Diffable diffable = (Diffable) object;
            DiffableScope scope = diffable.scope;

            if (scope != null) {
                diffable.initialize(scope.resolve());
            }

            for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
                if (field.shouldBeDiffed()) {
                    resolve(field.getValue(diffable));
                }
            }

        } else if (object instanceof Collection) {
            for (Object item : (Collection<?>) object) {
                resolve(item);
            }
        }
    }

}
