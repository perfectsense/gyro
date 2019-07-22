package gyro.core.validation;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.lang.GyroCharStream;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import org.apache.commons.lang3.StringUtils;

public class ValidationError implements Locatable {

    private final Diffable diffable;
    private final String fieldName;
    private final List<String> messages;
    private final Node node;

    public ValidationError(Diffable diffable, String fieldName, List<String> messages) {
        this.diffable = diffable;
        this.fieldName = fieldName;
        this.messages = ImmutableList.copyOf(messages);
        this.node = DiffableInternals.getScope(diffable).getValueNodes().get(fieldName);
    }

    public void write(GyroUI ui) {
        Diffable parent = diffable.parent();
        DiffableType type = DiffableType.getInstance(diffable.getClass());
        String name = DiffableInternals.getName(diffable);
        String label;

        if (parent == null) {
            label = String.format("%s %s", type.getName(), name);

        } else {
            label = name;
            String primaryKey = diffable.primaryKey();

            if (!StringUtils.isBlank(primaryKey)) {
                label += " ";
                label += primaryKey;
            }

            label += String.format(
                " for %s %s",
                DiffableType.getInstance(parent.getClass()).getName(),
                DiffableInternals.getName(parent));
        }

        ui.write("\n@|bold %s|@ field in @|bold %s|@", fieldName, label);
        Optional.ofNullable(toLocation()).ifPresent(s -> ui.write(" %s", s));
        ui.write(":\n");
        Optional.ofNullable(toCodeSnippet()).ifPresent(s -> ui.write("%s\n", s));

        for (String message : messages) {
            ui.write("· %s\n", message);
        }
    }

    @Override
    public GyroCharStream getStream() {
        return node != null ? node.getStream() : null;
    }

    @Override
    public int getStartLine() {
        return node != null ? node.getStartLine() : -1;
    }

    @Override
    public int getStartColumn() {
        return node != null ? node.getStartColumn() : -1;
    }

    @Override
    public int getStopLine() {
        return node != null ? node.getStopLine() : -1;
    }

    @Override
    public int getStopColumn() {
        return node != null ? node.getStopColumn() : -1;
    }

}
