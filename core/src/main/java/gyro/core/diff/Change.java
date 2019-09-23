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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.scope.State;
import org.apache.commons.lang3.StringUtils;

public abstract class Change {

    private final List<Diff> diffs = new ArrayList<>();
    final AtomicBoolean changed = new AtomicBoolean();

    public List<Diff> getDiffs() {
        return diffs;
    }

    public abstract Diffable getDiffable();

    public abstract void writePlan(GyroUI ui);

    public abstract void writeExecution(GyroUI ui);

    public String getLabel(Diffable diffable, boolean includeParent) {
        Diffable parent = diffable.parent();
        DiffableType<Diffable> type = DiffableType.getInstance(diffable);
        String name = DiffableInternals.getName(diffable);
        String label;

        if (parent == null) {
            label = String.format("%s %s", type.getName(), name);

            DiffableField idField = type.getIdField();

            if (idField != null) {
                Object id = idField.getValue(diffable);

                if (id != null) {
                    label += " (";
                    label += id;
                    label += ")";
                }
            }

        } else {
            label = name;
            String primaryKey = diffable.primaryKey();

            if (!StringUtils.isBlank(primaryKey)) {
                label += " ";
                label += primaryKey;
            }

            if (includeParent) {
                label += String.format(
                    " for %s %s",
                    DiffableType.getInstance(parent.getClass()).getName(),
                    DiffableInternals.getName(parent));
            }
        }

        String description = type.getDescription(diffable);

        if (!StringUtils.isBlank(description)) {
            return label + ":|@ @|reset " + description;

        } else {
            return label;
        }
    }

    public abstract ExecutionResult execute(
        GyroUI ui,
        State state,
        List<ChangeProcessor> processors) throws Exception;

    protected String stringify(Object value) {
        if (value instanceof Collection) {
            return "[ " + ((Collection<?>) value).stream()
                .map(this::stringify)
                .collect(Collectors.joining(", ")) + " ]";

        } else if (value instanceof Map) {
            return "{ " + ((Map<?, ?>) value).entrySet()
                .stream()
                .map(e -> e.getKey() + ": " + stringify(e.getValue()))
                .collect(Collectors.joining(", ")) + " }";

        } else if (value instanceof String) {
            return "'" + value + "'";

        } else {
            return String.valueOf(value);
        }
    }

    protected void writeDifference(
        GyroUI ui,
        DiffableField field,
        Diffable currentDiffable,
        Diffable pendingDiffable) {

        ui.write("\n· %s: ", field.getName());

        Object currentValue = field.getValue(currentDiffable);
        Object pendingValue = field.getValue(pendingDiffable);

        if ((currentValue == null || currentValue instanceof Collection)
            && (pendingValue == null || pendingValue instanceof Collection)) {

            List<?> currentList =
                currentValue != null ? new ArrayList<>((Collection<?>) currentValue) : new ArrayList<>();
            List<?> pendingList =
                pendingValue != null ? new ArrayList<>((Collection<?>) pendingValue) : new ArrayList<>();
            int currentListSize = currentList.size();
            int pendingListSize = pendingList.size();

            for (int i = 0, l = Math.max(currentListSize, pendingListSize); i < l; ++i) {
                Object c = i < currentListSize ? currentList.get(i) : null;
                Object p = i < pendingListSize ? pendingList.get(i) : null;

                if (Objects.equals(c, p)) {
                    ui.write(stringify(c));

                } else if (c == null) {
                    ui.write(" @|green +|@ %s", stringify(p));

                } else if (p == null) {
                    ui.write(" @|red -|@ %s", stringify(c));

                } else {
                    ui.write(" @|yellow ⟳|@ %s → %s", stringify(c), stringify(p));
                }

                if (i < l - 1) {
                    ui.write(",");
                }
            }

        } else if ((currentValue == null || currentValue instanceof Map)
            && (pendingValue == null || pendingValue instanceof Map)) {

            if (currentValue == null) {
                writeMapPut(ui, (Map<?, ?>) pendingValue);

            } else if (pendingValue == null) {
                writeMapRemove(ui, (Map<?, ?>) currentValue);

            } else {
                MapDifference<?, ?> diff = Maps.difference((Map<?, ?>) currentValue, (Map<?, ?>) pendingValue);

                writeMapRemove(ui, diff.entriesOnlyOnLeft());

                writeMap(
                    ui,
                    " @|yellow ⟳ {|@ %s @|yellow }|@",
                    diff.entriesDiffering(),
                    e -> String.format(
                        "%s → %s",
                        stringify(e.leftValue()),
                        stringify(e.rightValue())));

                writeMapPut(ui, diff.entriesOnlyOnRight());
            }

        } else {
            ui.write(" %s → %s", stringify(currentValue), stringify(pendingValue));
        }
    }

    private <V> void writeMap(GyroUI ui, String message, Map<?, V> map, Function<V, String> valueFunction) {
        if (map.isEmpty()) {
            return;
        }

        ui.write(message, map.entrySet()
            .stream()
            .map(e -> e.getKey() + ": " + valueFunction.apply(e.getValue()))
            .collect(Collectors.joining(", ")));
    }

    private void writeMapPut(GyroUI ui, Map<?, ?> map) {
        writeMap(ui, " @|green +{|@ %s @|green }|@", map, this::stringify);
    }

    private void writeMapRemove(GyroUI ui, Map<?, ?> map) {
        writeMap(ui, " @|red -{|@ %s @|red }|@", map, this::stringify);
    }

}
