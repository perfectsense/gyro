package gyro.core.resource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import gyro.core.diff.Change;
import gyro.core.scope.DiffableScope;

public final class DiffableInternals {

    private DiffableInternals() {
    }

    public static boolean isExternal(Diffable diffable) {
        return diffable.external;
    }

    public static void setExternal(Diffable diffable, boolean external) {
        diffable.external = external;
    }

    public static String getName(Diffable diffable) {
        return diffable.name;
    }

    public static void setName(Diffable diffable, String name) {
        diffable.name = name;
    }

    public static DiffableScope getScope(Diffable diffable) {
        return diffable.scope;
    }

    public static Set<String> getConfiguredFields(Diffable diffable) {
        if (diffable.configuredFields == null) {
            diffable.configuredFields = new LinkedHashSet<>();
        }

        return diffable.configuredFields;
    }

    public static Change getChange(Diffable diffable) {
        return diffable.change;
    }

    public static void setChange(Diffable diffable, Change change) {
        diffable.change = change;
    }

    public static void update(Diffable diffable, boolean newScope) {
        if (newScope) {
            diffable.scope = new DiffableScope(diffable.scope.getParent(), null);
        }

        updateChildren(diffable, newScope);
    }

    private static void updateChildren(Diffable diffable, boolean newScope) {
        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (field.shouldBeDiffed()) {
                String fieldName = field.getName();
                Object value = field.getValue(diffable);

                (value instanceof Collection ? ((Collection<?>) value).stream() : Stream.of(value))
                    .filter(Diffable.class::isInstance)
                    .map(Diffable.class::cast)
                    .forEach(d -> {
                        if (newScope) {
                            d.scope = new DiffableScope(diffable.scope, null);
                        }

                        d.parent = diffable;
                        d.name = fieldName;

                        updateChildren(d, newScope);
                    });
            }
        }
    }
}
