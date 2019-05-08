package gyro.core.resource;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.psddev.dari.util.TypeDefinition;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.diff.Change;
import gyro.lang.ast.Node;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class Diffable {

    private DiffableScope scope;
    private Diffable parent;
    private String name;
    private Change change;
    private Set<String> configuredFields;

    public DiffableScope scope() {
        return scope;
    }

    public void scope(DiffableScope scope) {
        this.scope = scope;
    }

    public Diffable parent() {
        return parent;
    }

    public Diffable parent(Diffable parent) {
        this.parent = parent;
        return this;
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

    public void name(String name) {
        this.name = name;
    }

    public Change change() {
        return change;
    }

    public void change(Change change) {
        this.change = change;
    }

    public Set<String> configuredFields() {
        return configuredFields != null ? configuredFields : Collections.emptySet();
    }

    public <T extends Resource> Stream<T> findByType(Class<T> resourceClass) {
        return scope.getRootScope()
            .findResources()
            .stream()
            .filter(resourceClass::isInstance)
            .map(resourceClass::cast);
    }

    public <T extends Resource> T findById(Class<T> resourceClass, String id) {
        DiffableField idField = DiffableType.getInstance(resourceClass).getIdField();

        return findByType(resourceClass)
            .filter(r -> id.equals(idField.getValue(r)))
            .findFirst()
            .orElseGet(() -> {
                T r = TypeDefinition.getInstance(resourceClass).newInstance();
                idField.setValue(r, id);
                return r;
            });
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
        diffable.scope(scope);
        diffable.parent(this);
        diffable.initialize(scope);

        return diffable;
    }

    public abstract String primaryKey();

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

        Resource otherResource = (Resource) other;

        return Objects.equals(parent(), otherResource.parent())
            && Objects.equals(name(), otherResource.name())
            && Objects.equals(primaryKey(), otherResource.primaryKey());
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
