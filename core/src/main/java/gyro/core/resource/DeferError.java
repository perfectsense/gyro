package gyro.core.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import gyro.lang.ast.Node;

public class DeferError extends Error {

    private final Node node;

    public DeferError(Node node) {
        this.node = node;
    }

    public static <T> void execute(List<T> items, Consumer<T> consumer) {
        int size = items.size();

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            List<T> deferred = new ArrayList<>();

            for (T item : items) {
                try {
                    consumer.accept(item);

                } catch (DeferError error) {
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

    @Override
    public String getMessage() {
        return node.deferFailure();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
