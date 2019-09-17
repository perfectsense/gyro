package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import gyro.core.GyroUI;
import gyro.lang.ast.Node;

public class Defer extends Error {

    private final Node node;

    public Defer(Node node, String message, Defer cause) {
        super(message, cause);

        this.node = node;
    }

    public Defer(Node node, String message) {
        this(node, message, null);
    }

    public static <T> void execute(List<T> items, Consumer<T> consumer) {
        int size = items.size();

        while (true) {
            List<Defer> errors = new ArrayList<>();
            List<T> deferred = new ArrayList<>();

            for (T item : items) {
                try {
                    consumer.accept(item);

                } catch (Defer error) {
                    errors.add(error);
                    deferred.add(item);
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (size == deferred.size()) {
                if (errors.size() == 1) {
                    throw errors.get(0);

                } else {
                    throw new MultipleDefers(errors);
                }

            } else {
                items = deferred;
                size = items.size();
            }
        }
    }

    public void write(GyroUI ui) {
        ui.write("@|red Error:|@ %s\n", getMessage());

        if (node != null) {
            ui.write("\nIn @|bold %s|@ %s:\n", node.getFile(), node.toLocation());
            ui.write("%s", node.toCodeSnippet());
        }

        Throwable cause = getCause();

        if (cause instanceof Defer) {
            ui.write("\n@|red Caused by:|@ ");
            ((Defer) cause).write(ui);
        }
    }

}
