package gyro.core.resource;

import java.util.Collection;
import java.util.stream.Stream;

public final class DiffableInternals {

    private DiffableInternals() {
    }

    public static void setName(Diffable diffable, String name) {
        diffable.name = name;
    }

    public static void update(Diffable diffable) {
        diffable.scope = new DiffableScope(diffable.scope().getParent());
        updateChildren(diffable);
    }

    private static void updateChildren(Diffable diffable) {
        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (field.shouldBeDiffed()) {
                String fieldName = field.getName();
                Object value = field.getValue(diffable);

                (value instanceof Collection ? ((Collection<?>) value).stream() : Stream.of(value))
                    .filter(Diffable.class::isInstance)
                    .map(Diffable.class::cast)
                    .forEach(d -> {
                        d.scope = new DiffableScope(diffable.scope());
                        d.parent = diffable;
                        d.name = fieldName;

                        updateChildren(d);
                    });
            }
        }
    }
}
