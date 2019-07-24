package gyro.core.validation;

import java.util.Optional;

import com.cronutils.utils.Preconditions;
import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.lang.GyroCharStream;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;

public class ValidationError implements Locatable {

    private final Diffable diffable;
    private final String fieldName;
    private final String message;
    private final Node node;

    public ValidationError(Diffable diffable, String fieldName, String message) {
        this.diffable = Preconditions.checkNotNull(diffable);
        this.fieldName = fieldName;
        this.message = message;
        this.node = DiffableInternals.getScope(diffable).getValueNodes().get(fieldName);
    }

    public void write(GyroUI ui) {
        ui.write("\n");

        Diffable parent = diffable.parent();

        if (parent == null) {
            ui.write(
                "%s %s",
                DiffableType.getInstance(diffable.getClass()).getName(),
                DiffableInternals.getName(diffable));

        } else {
            ui.write(
                "%s %s → %s %s",
                DiffableType.getInstance(parent.getClass()).getName(),
                DiffableInternals.getName(parent),
                DiffableInternals.getName(diffable),
                diffable.primaryKey());
        }

        Optional.ofNullable(fieldName).ifPresent(s -> ui.write(" → %s", s));
        ui.write(": %s\n", message);
        Optional.ofNullable(toLocation()).ifPresent(s -> ui.write("\nIn @|bold %s|@ %s\n", getFile(), s));
        Optional.ofNullable(toCodeSnippet()).ifPresent(s -> ui.write("%s", s));
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
