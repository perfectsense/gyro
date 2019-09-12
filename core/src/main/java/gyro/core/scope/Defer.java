package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;
import gyro.lang.ast.Node;
import gyro.util.ImmutableCollectors;

public class Defer extends Error {

    private final List<Node> nodes;

    public Defer(Node node, String message) {
        super(message);

        this.nodes = ImmutableList.of(node);
    }

    private Defer(List<Defer> errors) {
        super("Circular dependencies detected!");

        this.nodes = errors.stream()
            .map(e -> e.nodes)
            .flatMap(List::stream)
            .collect(ImmutableCollectors.toList());
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
                throw new Defer(errors);

            } else {
                items = deferred;
                size = items.size();
            }
        }
    }

    public void write(GyroUI ui) {
        ui.write("@|red Error:|@ %s\n", getMessage());

        for (Node node : nodes) {
            ui.write("\nIn @|bold %s|@ %s:\n", node.getFile(), node.toLocation());
            ui.write("%s", node.toCodeSnippet());
        }
    }

}
