package beam.lang;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import beam.core.diff.Diffable;
import beam.core.diff.DiffableField;
import beam.core.diff.DiffableType;
import beam.core.diff.ResourceName;
import beam.lang.ast.block.ResourceNode;
import beam.lang.ast.scope.ResourceScope;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.value.StringNode;

public abstract class Resource extends Diffable {

    private String type;
    private String name;
    private ResourceScope scope;
    private Resource parent;

    // -- Resource Implementation API

    public abstract boolean refresh();

    public abstract void create();

    public abstract void update(Resource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract Class resourceCredentialsClass();

    public ResourceScope scope() {
        return scope;
    }

    public void scope(ResourceScope scope) {
        this.scope = scope;
    }

    public Resource parent() {
        return parent;
    }

    public Resource parent(Resource parent) {
        this.parent = parent;
        return this;
    }

    public Credentials resourceCredentials() {
        for (Resource r = this; r != null; r = r.parent()) {
            Scope scope = r.scope();

            if (scope != null) {
                String name = (String) scope.get("resource-credentials");
                return scope.getRootScope().getCredentialsMap().get(name != null ? name : "default");
            }
        }

        throw new IllegalStateException();
    }

    // -- Diff Engine

    public String primaryKey() {
        return String.format("%s %s", resourceType(), resourceIdentifier());
    }

    // -- Base Resource

    public Object get(String key) {
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getFieldByBeamName(key))
                .map(f -> f.getValue(this))
                .orElse(null);
    }

    public String resourceType() {
        if (type == null) {
            ResourceName name = getClass().getAnnotation(ResourceName.class);
            return name != null ? name.value() : null;
        }

        return type;
    }

    public void resourceType(String type) {
        this.type = type;
    }

    public String resourceIdentifier() {
        return name;
    }

    public void resourceIdentifier(String name) {
        this.name = name;
    }

    public void initialize(Map<String, Object> values) {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();

            if (values.containsKey(key)) {
                field.setValue(this, values.get(key));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Resource that = (Resource) o;

        return Objects.equals(primaryKey(), that.primaryKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryKey());
    }

    public ResourceNode toNode() {
        return new ResourceNode(
                resourceType(),
                new StringNode(resourceIdentifier()),
                toBodyNodes());
    }

    @Override
    public String toString() {
        if (resourceIdentifier() == null) {
            return String.format("Resource[type: %s]", resourceType());
        }

        return String.format("Resource[type: %s, id: %s]", resourceType(), resourceIdentifier());
    }

}
