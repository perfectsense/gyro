/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class Diff {

    protected final List<Diffable> currentDiffables;
    private final List<Diffable> pendingDiffables;
    private final List<Change> changes = new ArrayList<>();

    public Diff(Collection<? extends Diffable> currentDiffables, Collection<? extends Diffable> pendingDiffables) {
        this.currentDiffables = currentDiffables != null
            ? new ArrayList<>(currentDiffables)
            : Collections.emptyList();

        this.pendingDiffables = pendingDiffables != null
            ? new ArrayList<>(pendingDiffables)
            : Collections.emptyList();

        this.currentDiffables.forEach(d -> DiffableInternals.update(d));
        this.pendingDiffables.forEach(d -> DiffableInternals.update(d));
    }

    public Diff(Diffable currentDiffable, Diffable pendingDiffable) {
        this.currentDiffables = currentDiffable != null
            ? Collections.singletonList(currentDiffable)
            : Collections.emptyList();

        this.pendingDiffables = pendingDiffable != null
            ? Collections.singletonList(pendingDiffable)
            : Collections.emptyList();

        this.currentDiffables.forEach(d -> DiffableInternals.update(d));
        this.pendingDiffables.forEach(d -> DiffableInternals.update(d));
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void diff() {
        diff(this.currentDiffables.stream());
    }

    public void diff(Stream<Diffable> currentDiffableStream) {
        Map<String, Diffable> currentDiffables = currentDiffableStream.collect(
            LinkedHashMap::new,
            (map, r) -> map.put(r.primaryKey(), r),
            Map::putAll
        );

        for (Diffable pendingDiffable : pendingDiffables) {
            if (skipDiffableDiff(pendingDiffable)) {
                continue;
            }

            if (pendingDiffable instanceof Resource) {
                DiffableInternals.reevaluate(pendingDiffable);
            }

            Diffable currentDiffable = currentDiffables.remove(pendingDiffable.primaryKey());

            changes.add(currentDiffable == null
                ? newCreate(pendingDiffable)
                : newUpdate(currentDiffable, pendingDiffable));
        }

        for (Diffable resource : currentDiffables.values()) {
            changes.add(newDelete(resource));
        }
    }

    public boolean skipDiffableDiff(Diffable pendingDiffable) {
        return false;
    }

    @SuppressWarnings("unchecked")
    private Change newCreate(Diffable diffable) {
        Create create = new Create(diffable);

        DiffableInternals.setChange(diffable, create);

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
        Set<String> currentConfiguredFields = DiffableInternals.getConfiguredFields(currentDiffable);
        Set<String> pendingConfiguredFields = DiffableInternals.getConfiguredFields(pendingDiffable);

        for (DiffableField field : type.getFields()) {
            if (!field.shouldBeDiffed()) {
                continue;
            }

            String name = field.getName();

            if (!currentConfiguredFields.contains(name) && !pendingConfiguredFields.contains(name)) {
                continue;
            }

            Object currentValue = field.getValue(currentDiffable);
            Object pendingValue = field.getValue(pendingDiffable);
            Diff diff;

            if (field.isCollection()) {
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
            .map(DiffableInternals::getName)
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

        DiffableInternals.setChange(currentDiffable, change);
        DiffableInternals.setChange(pendingDiffable, change);
        change.getDiffs().addAll(diffs);

        return change;
    }

    private Set<DiffableField> diffFields(Diffable currentDiffable, Diffable pendingDiffable) {
        Set<String> currentConfiguredFields = DiffableInternals.getConfiguredFields(currentDiffable);
        Set<String> pendingConfiguredFields = DiffableInternals.getConfiguredFields(pendingDiffable);
        Set<DiffableField> changedFields = new LinkedHashSet<>();

        for (DiffableField field : DiffableType.getInstance(currentDiffable.getClass()).getFields()) {

            // Skip nested diffables since they're handled by the diff system.
            if (field.shouldBeDiffed()) {
                continue;
            }

            // Skip the field whose value can be modified by a cloud provider.
            if (field.isImmutable()) {
                continue;
            }

            String name = field.getName();

            // Skip if there isn't a pending value and the field wasn't
            // previously configured. This means that a field was
            // automatically populated in code so we should keep it as is.
            if (!currentConfiguredFields.contains(name) && !pendingConfiguredFields.contains(name)) {
                continue;
            }

            Object currentValue = field.getValue(currentDiffable);
            Object pendingValue = field.getValue(pendingDiffable);

            // Skip if the value didn't change.
            if (Objects.equals(currentValue, pendingValue)) {
                continue;
            }

            changedFields.add(field);
        }

        return changedFields;
    }

    @SuppressWarnings("unchecked")
    private Change newDelete(Diffable diffable) {
        Delete delete = new Delete(diffable);

        reevaluateStateNodesFromState(diffable);

        DiffableInternals.setChange(diffable, delete);

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

    private void reevaluateStateNodesFromState(Diffable diffable) {
        DiffableScope scope = DiffableInternals.getScope(diffable);

        List<Node> nodes = scope.getStateNodes();

        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        nodes.forEach(node -> evaluator.visit(node, scope));
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
        return write(ui, false);
    }

    public boolean write(GyroUI ui, boolean written) {
        if (!hasChanges()) {
            return false;
        }

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

    public void execute(GyroUI ui, State state) {
        preDeleteExecute(ui, state);
        executeDelete(ui, state);
    }

    public void preDeleteExecute(GyroUI ui, State state) {
        executeCreateKeepUpdate(ui, state);
        executeReplace(ui, state);
    }

    private void executeCreateKeepUpdate(GyroUI ui, State state) {
        for (Change change : getChanges()) {
            if (change instanceof Create || change instanceof Keep || change instanceof Update) {
                executeChange(ui, state, change);
            }

            for (Diff d : change.getDiffs()) {
                d.executeCreateKeepUpdate(ui, state);
            }
        }
    }

    private void executeReplace(GyroUI ui, State state) {
        for (Change change : getChanges()) {
            if (change instanceof Replace) {
                executeChange(ui, state, change);
            }

            for (Diff d : change.getDiffs()) {
                d.executeReplace(ui, state);
            }
        }
    }

    private void executeDelete(GyroUI ui, State state) {
        for (ListIterator<Change> j = getChanges().listIterator(getChanges().size()); j.hasPrevious(); ) {
            Change change = j.previous();

            for (Diff d : change.getDiffs()) {
                d.executeDelete(ui, state);
            }

            if (change instanceof Delete) {
                executeChange(ui, state, change);
            }
        }
    }

    private void executeChange(GyroUI ui, State state, Change change) {
        Diffable diffable = change.getDiffable();

        if (!(diffable instanceof Resource)) {
            return;
        }

        if (change.changed.compareAndSet(false, true)) {
            DiffableInternals.reevaluate(diffable);

            if (!diffable.writeExecution(ui, change)) {
                change.writeExecution(ui);
            }

            ExecutionResult result;

            try {
                List<ChangeProcessor> processors = new ArrayList<>();

                for (Scope s = DiffableInternals.getScope(diffable); s != null; s = s.getParent()) {
                    processors.addAll(0, s.getSettings(ChangeSettings.class).getProcessors());
                }

                result = change.execute(ui, state, processors);

            } catch (Exception error) {
                throw new GyroException(
                    String.format(
                        "Can't %s @|bold %s|@ resource!",
                        change.getClass().getSimpleName().toLowerCase(Locale.ENGLISH),
                        diffable),
                    error);
            }

            state.save();

            if (result != null) {
                result.write(ui);
            }
        }
    }

}
