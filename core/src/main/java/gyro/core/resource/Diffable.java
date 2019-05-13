package gyro.core.resource;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.lang.ast.Node;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class Diffable {

    boolean external;
    DiffableScope scope;
    Diffable parent;
    String name;
    Change change;
    Set<String> configuredFields;

    public Diffable parent() {
        return parent;
    }

    public Resource parentResource() {
        for (Diffable d = parent(); d != null; d = d.parent()) {
            if (d instanceof Resource) {
                return (Resource) d;
            }
        }

        return null;
    }

    public String name() {
        return name;
    }

    public Change change() {
        return change;
    }

    public <T extends Resource> Stream<T> findByClass(Class<T> resourceClass) {
        return scope.getRootScope().findResourcesByClass(resourceClass);
    }

    public <T extends Resource> T findById(Class<T> resourceClass, Object id) {
        return scope.getRootScope().findResourceById(resourceClass, id);
    }

    public InputStream openInput(String file) {
        FileScope fileScope = scope.getFileScope();

        return fileScope.getRootScope()
            .getBackend()
            .openInput(Paths.get(fileScope.getFile())
                .getParent()
                .resolve(file)
                .toString());
    }

    public void initialize(Map<String, Object> values) {
        if (configuredFields == null) {

            // Current state contains an explicit list of configured fields
            // that were in the original diffable definition.
            @SuppressWarnings("unchecked")
            Collection<String> cf = (Collection<String>) values.get("_configured-fields");

            if (cf == null) {

                // Only save fields that are in the diffable definition and
                // exclude the ones that were copied from the current state.
                if (values instanceof DiffableScope) {
                    cf = ((DiffableScope) values).getAddedKeys();

                } else {
                    cf = values.keySet();
                }
            }

            configuredFields = ImmutableSet.copyOf(cf);
        }

        Map<String, Object> undefinedValues = new HashMap<>(values);
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String fieldName = field.getName();

            if (!values.containsKey(fieldName)) {
                continue;
            }

            Object value = values.get(fieldName);

            if (field.shouldBeDiffed()) {
                @SuppressWarnings("unchecked")
                Class<? extends Diffable> diffableClass = (Class<? extends Diffable>) field.getItemClass();

                if (value instanceof Collection) {
                    value = ((Collection<?>) value).stream()
                            .map(v -> toDiffable(fieldName, diffableClass, v))
                            .collect(Collectors.toList());

                } else if (value instanceof DiffableScope) {
                    value = toDiffable(fieldName, diffableClass, value);
                }
            }

            field.setValue(this, value);
            undefinedValues.remove(fieldName);
        }

        for (Map.Entry<String, Object> entry : undefinedValues.entrySet()) {
            if (!entry.getKey().startsWith("_")) {
                if (values instanceof Scope) {
                    Node node = ((Scope) values).getKeyNodes().get(entry.getKey());
                    if (node != null) {
                        throw new GyroException(String.format("Field '%s' is not allowed %s%n%s", entry.getKey(), node.getLocation(), node));
                    }
                }

                throw new GyroException(String.format("Field '%s' is not allowed", entry.getKey()));
            }
        }
    }

    private Object toDiffable(String fieldName, Class<? extends Diffable> diffableClass, Object object) {
        if (!(object instanceof DiffableScope)) {
            return object;
        }

        DiffableScope scope = (DiffableScope) object;
        Diffable diffable;

        try {
            diffable = diffableClass.getConstructor().newInstance();

        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }

        diffable.name = fieldName;
        diffable.scope = scope;
        diffable.parent = this;
        diffable.initialize(scope);

        return diffable;
    }

    public String primaryKey() {
        return String.format("%s::%s", DiffableType.getInstance(getClass()).getName(), name());
    }

    public abstract String toDisplayString();

    public boolean writePlan(GyroUI ui, Change change) {
        return false;
    }

    public boolean writeExecution(GyroUI ui, Change change) {
        return false;
    }

    public void updateInternals() {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            if (field.shouldBeDiffed()) {
                String fieldName = field.getName();
                Object value = field.getValue(this);

                (value instanceof Collection ? ((Collection<?>) value).stream() : Stream.of(value))
                    .filter(Diffable.class::isInstance)
                    .map(Diffable.class::cast)
                    .forEach(d -> {
                        d.parent = this;
                        d.name = fieldName;

                        d.updateInternals();
                    });
            }
        }
    }

    @Override
    public final int hashCode() {
        return Objects.hash(parent(), name(), primaryKey());
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Diffable otherDiffable = (Diffable) other;

        if (external) {
            DiffableField idField = DiffableType.getInstance(getClass()).getIdField();
            return Objects.equals(idField.getValue(this), idField.getValue(otherDiffable));

        } else {
            return Objects.equals(parent(), otherDiffable.parent())
                && Objects.equals(name(), otherDiffable.name())
                && Objects.equals(primaryKey(), otherDiffable.primaryKey());
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("parent", parent())
            .append("name", name())
            .append("primaryKey", primaryKey())
            .build();
    }

}
