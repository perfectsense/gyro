package gyro.core.resource;

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
import gyro.core.GyroInputStream;
import gyro.core.GyroUI;
import gyro.core.diff.Change;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.scope.Scope;

public abstract class Diffable {

    boolean external;
    Diffable parent;
    String name;
    DiffableScope scope;
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

    public <T extends Resource> Stream<T> findByClass(Class<T> resourceClass) {
        return scope.getRootScope().findResourcesByClass(resourceClass);
    }

    public <T extends Resource> T findById(Class<T> resourceClass, Object id) {
        return scope.getRootScope().findResourceById(resourceClass, id);
    }

    public GyroInputStream openInput(String file) {
        FileScope fileScope = scope.getFileScope();

        return fileScope.getRootScope()
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

        DiffableType<? extends Diffable> type = DiffableType.getInstance(getClass());
        Map<String, Object> undefinedValues = new HashMap<>(values);

        for (DiffableField field : type.getFields()) {
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
            String key = entry.getKey();

            if (!key.startsWith("_")) {
                throw new GyroException(
                    values instanceof Scope ? ((Scope) values).getKeyNodes().get(key) : null,
                    String.format("@|bold %s|@ isn't a valid field in @|bold %s|@ type!", key, type.getName()));
            }
        }
    }

    private Object toDiffable(String fieldName, Class<? extends Diffable> diffableClass, Object object) {
        if (!(object instanceof DiffableScope)) {
            return object;
        }

        DiffableScope scope = (DiffableScope) object;
        Diffable diffable = DiffableType.getInstance(diffableClass).newDiffable(this, fieldName, scope);

        diffable.initialize(scope);

        return diffable;
    }

    protected <T extends Diffable> T newSubresource(Class<T> diffableClass) {
        return DiffableType.getInstance(diffableClass).newDiffable(this, null, new DiffableScope(scope));
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
        StringBuilder builder = new StringBuilder();
        DiffableType type = DiffableType.getInstance(getClass());
        String typeName = type.getName();

        if (typeName != null) {
            builder.append(typeName);
            builder.append(' ');

            if (external) {
                builder.append("id=");
                builder.append(type.getIdField().getValue(this));

            } else {
                builder.append(name);
            }

        } else {
            builder.append(name);
            builder.append(' ');
            builder.append(primaryKey());
        }

        return builder.toString();
    }

}
