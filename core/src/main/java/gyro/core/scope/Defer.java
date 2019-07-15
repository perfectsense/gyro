package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import gyro.lang.ast.Node;

public class Defer extends Error {

    private final Node node;

    public Defer(Node node, String message) {
        super(message);
        this.node = node;
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
                throw errors.get(0);

            } else {
                items = deferred;
                size = items.size();
            }
        }
    }

    public Node getNode() {
        return node;
    }

}
