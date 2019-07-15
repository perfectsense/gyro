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
import gyro.core.scope.State;

public abstract class Change {

    private final List<Diff> diffs = new ArrayList<>();
    final AtomicBoolean changed = new AtomicBoolean();

    public List<Diff> getDiffs() {
        return diffs;
    }

    public abstract Diffable getDiffable();

    public abstract void writePlan(GyroUI ui);

    public abstract void writeExecution(GyroUI ui);

    public abstract ExecutionResult execute(GyroUI ui, State state) throws Exception;

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
