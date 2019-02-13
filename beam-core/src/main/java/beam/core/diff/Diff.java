package beam.core.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import beam.core.BeamCore;
import beam.lang.Credentials;
import beam.lang.Resource;
import beam.lang.ast.scope.State;
import com.psddev.dari.util.ObjectUtils;

public class Diff {

    private final List<Diffable> currentDiffables;
    private final List<Diffable> pendingDiffables;
    private final List<Change> changes = new ArrayList<>();

    public Diff(List<? extends Diffable> currentDiffables, List<? extends Diffable> pendingDiffables) {
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

    public void diff() throws Exception {
        Map<String, Diffable> currentDiffables = this.currentDiffables.stream().collect(
            LinkedHashMap::new,
            (map, r) -> map.put(r.primaryKey(), r),
            Map::putAll
        );

        for (Diffable pendingDiffable : pendingDiffables) {
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
    private Change newCreate(Diffable diffable) throws Exception {
        Create create = new Create(diffable);

        diffable.change(create);

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!Diffable.class.isAssignableFrom(field.getItemClass())) {
                continue;
            }

            Object value = field.getValue(diffable);
            Diff diff;

            if (value instanceof List) {
                diff = new Diff(null, (List<Diffable>) value);

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
    private Change newUpdate(Diffable currentDiffable, Diffable pendingDiffable) throws Exception {
        List<Diff> diffs = new ArrayList<>();

        for (DiffableField field : DiffableType.getInstance(currentDiffable.getClass()).getFields()) {
            if (!Diffable.class.isAssignableFrom(field.getItemClass())) {
                continue;
            }

            Object currentValue = field.getValue(currentDiffable);
            Object pendingValue = field.getValue(pendingDiffable);
            Diff diff;

            if (pendingValue instanceof List) {
                diff = new Diff((List<Diffable>) currentValue, (List<Diffable>) pendingValue);

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

        ResourceDisplayDiff displayDiff = diffFields(currentDiffable, pendingDiffable);
        Set<String> changedProperties = displayDiff.getChangedProperties();
        String changedDisplay = displayDiff.getChangedDisplay().toString();
        Change change;

        if (changedProperties.isEmpty() && !hasNonResourceChanges(diffs)) {
            change = new Keep(pendingDiffable);

        } else if (displayDiff.isReplace()) {
            change = new Replace(currentDiffable, pendingDiffable, changedProperties, changedDisplay);

        } else {
            change = new Update(currentDiffable, pendingDiffable, changedProperties, changedDisplay);
        }

        currentDiffable.change(change);
        pendingDiffable.change(change);
        change.getDiffs().addAll(diffs);

        return change;
    }

    private boolean hasNonResourceChanges(List<Diff> diffs) {
        for (Diff diff : diffs) {
            for (Change change : diff.getChanges()) {
                if (!(change instanceof Keep)
                        && !(change.getDiffable() instanceof Resource)) {

                    return true;
                }

                if (hasNonResourceChanges(change.getDiffs())) {
                    return true;
                }
            }
        }

        return false;
    }

    private ResourceDisplayDiff diffFields(Diffable currentDiffable, Diffable pendingDiffable) {
        Set<String> currentConfiguredFields = currentDiffable.configuredFields();
        ResourceDisplayDiff displayDiff = new ResourceDisplayDiff();

        for (DiffableField field : DiffableType.getInstance(currentDiffable.getClass()).getFields()) {

            // Skip nested diffables since they're handled by the diff system.
            if (Diffable.class.isAssignableFrom(field.getItemClass())) {
                continue;
            }

            Object currentValue = field.getValue(currentDiffable);
            Object pendingValue = field.getValue(pendingDiffable);

            // Skip if the value didn't change.
            if (Objects.equals(currentValue, pendingValue)) {
                continue;
            }

            String key = field.getBeamName();

            // Skip if there isn't a pending value and the field wasn't
            // previously configured. This means that a field was
            // automatically populated in code so we should keep it as is.
            if (pendingValue == null && !currentConfiguredFields.contains(key)) {
                continue;
            }

            String output;

            if (pendingValue instanceof List) {
                output = Change.processAsListValue(key, (List) currentValue, (List) pendingValue);

            } else if (pendingValue instanceof Map) {
                output = Change.processAsMapValue(key, (Map) currentValue, (Map) pendingValue);

            } else {
                output = Change.processAsScalarValue(key, currentValue, pendingValue);
            }

            if (output.length() > 0) {
                displayDiff.addChangedProperty(key);
                displayDiff.getChangedDisplay().append(output).append(", ");

                if (!field.isUpdatable()) {
                    displayDiff.setReplace(true);
                }
            }
        }

        StringBuilder display = displayDiff.getChangedDisplay();
        int displayLength = display.length();

        if (displayLength > 0) {
            display.setLength(displayLength - 2);
        }

        return displayDiff;
    }

    @SuppressWarnings("unchecked")
    private Change newDelete(Diffable diffable) throws Exception {
        Delete delete = new Delete(diffable);

        diffable.change(delete);

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!Diffable.class.isAssignableFrom(field.getItemClass())) {
                continue;
            }

            Object value = field.getValue(diffable);
            Diff diff;

            if (value instanceof List) {
                diff = new Diff((List<Diffable>) value, null);

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

    public Set<ChangeType> write() {
        Set<ChangeType> changeTypes = new HashSet<>();

        if (!hasChanges()) {
            return changeTypes;
        }

        for (Change change : getChanges()) {
            if (change.getDiffable() instanceof Credentials) {
                continue;
            }

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

            if (change instanceof Create) {
                changeTypes.add(ChangeType.CREATE);

            } else if (change instanceof Delete) {
                changeTypes.add(ChangeType.DELETE);

            } else if (change instanceof Keep) {
                changeTypes.add(ChangeType.KEEP);

            } else if (change instanceof Replace) {
                changeTypes.add(ChangeType.REPLACE);

            } else if (change instanceof Update) {
                changeTypes.add(ChangeType.UPDATE);
            }

            change.writeTo(BeamCore.ui());
            BeamCore.ui().write("\n");
            BeamCore.ui().indented(() -> changeDiffs.forEach(d -> changeTypes.addAll(d.write())));
        }

        return changeTypes;
    }

    private void setChangeable() {
        for (Change change : getChanges()) {
            change.setExecutable(true);
            change.getDiffs().forEach(Diff::setChangeable);
        }
    }

    public void executeCreateOrUpdate(State state) throws Exception {
        setChangeable();

        for (Change change : getChanges()) {
            if (change instanceof Create || change instanceof Update) {
                Diffable diffable = change.getDiffable();

                if (diffable instanceof Resource) {
                    if (!(change.getDiffable() instanceof Credentials)) {
                        execute(change);
                    }

                    state.update(change);
                }
            }

            for (Diff d : change.getDiffs()) {
                d.executeCreateOrUpdate(state);
            }
        }
    }

    public void executeDelete(State state) throws Exception {
        setChangeable();

        for (ListIterator<Change> j = getChanges().listIterator(getChanges().size()); j.hasPrevious();) {
            Change change = j.previous();

            for (Diff d : change.getDiffs()) {
                d.executeDelete(state);
            }

            if (change instanceof Delete) {
                Diffable diffable = change.getDiffable();

                if (diffable instanceof Resource) {
                    if (!(diffable instanceof Credentials)) {
                        execute(change);
                    }

                    state.update(change);
                }
            }
        }
    }

    private void execute(Change change) throws Exception {
        if (change instanceof Keep || change instanceof Replace || change.isChanged()) {
            return;
        }

        BeamCore.ui().write("Executing: ");
        change.writeTo(BeamCore.ui());
        change.execute();
        BeamCore.ui().write(" OK\n");
    }

}
