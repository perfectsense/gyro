package beam.core.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import beam.core.BeamUI;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public abstract class Change {

    private final List<Diff> diffs = new ArrayList<>();
    boolean executable;
    final AtomicBoolean changed = new AtomicBoolean();

    public List<Diff> getDiffs() {
        return diffs;
    }

    public abstract Diffable getDiffable();

    public abstract void writePlan(BeamUI ui);

    public abstract void writeExecution(BeamUI ui);

    public abstract void execute();

    protected void writeDifference(
            BeamUI ui,
            DiffableField field,
            Diffable currentDiffable,
            Diffable pendingDiffable) {

        ui.write("\n· %s:", field.getBeamName());

        Object currentValue = field.getValue(currentDiffable);
        Object pendingValue = field.getValue(pendingDiffable);

        if ((currentValue == null || currentValue instanceof List)
                && (pendingValue == null || pendingValue instanceof List)) {

            List<?> currentList = (List<?>) currentValue;
            List<?> pendingList = (List<?>) pendingValue;
            List<?> removed = currentList != null ? new ArrayList<>(currentList) : new ArrayList<>();
            List<?> added = pendingList != null ? new ArrayList<>(pendingList) : new ArrayList<>();

            if (pendingList != null) {
                removed.removeAll(pendingList);
            }

            if (currentList != null) {
                added.removeAll(currentList);
            }

            writeList(ui, " @|red -[|@ %s @|red ]|@", removed);
            writeList(ui, " @|green +[|@ %s @|green ]|@", added);

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
                    e -> String.format("%s → %s", e.leftValue(), e.rightValue()));

                writeMapPut(ui, diff.entriesOnlyOnRight());
            }

        } else {
            ui.write(" %s → %s", currentValue, pendingValue);
        }
    }

    private void writeList(BeamUI ui, String message, List<?> list) {
        if (list.isEmpty()) {
            return;
        }

        ui.write(message, list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
    }

    private <V> void writeMap(BeamUI ui, String message, Map<?, V> map, Function<V, String> valueFunction) {
        if (map.isEmpty()) {
            return;
        }

        ui.write(message, map.entrySet()
                .stream()
                .map(e -> e.getKey() + ": " + valueFunction.apply(e.getValue()))
                .collect(Collectors.joining(", ")));
    }

    private void writeMapPut(BeamUI ui, Map<?, ?> map) {
        writeMap(ui, " @|green +{|@ %s @|green }|@", map, String::valueOf);
    }

    private void writeMapRemove(BeamUI ui, Map<?, ?> map) {
        writeMap(ui, " @|red -{|@ %s @|red }|@", map, String::valueOf);
    }

}
