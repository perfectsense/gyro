package gyro.lang;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.diff.ResourceName;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ast.value.LiteralStringNode;

public abstract class Resource extends Diffable {

    private String type;
    private String name;

    // -- Resource Implementation API

    public abstract boolean refresh();

    public abstract void create();

    public void testCreate() {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            if (field.getTestValue() != null) {
                String value = "test-" + field.getTestValue();

                if (field.isTestValueRandomSuffix())  {
                    value += "-";
                    value += UUID.randomUUID().toString().replaceAll("-", "").substring(16);
                }

                field.setValue(this, value);
            }
        }
    }

    public abstract void update(Resource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract Class resourceCredentialsClass();

    public Credentials resourceCredentials() {
        for (Resource r = this; r != null; r = r.parentResource()) {
            Scope scope = r.scope();

            if (scope != null) {
                String name = (String) scope.get("resource-credentials");

                if (name == null) {
                    name = "default";
                }

                for (Resource resource : scope.getRootScope().findAllResources()) {
                    if (resource instanceof Credentials) {
                        Credentials credentials = (Credentials) resource;

                        if (credentials.resourceIdentifier().equals(name)) {
                            return credentials;
                        }
                    }
                }
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
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getFieldByGyroName(key))
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
                new LiteralStringNode(resourceIdentifier()),
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
