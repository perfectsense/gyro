package beam.lang;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import beam.core.diff.Change;
import beam.core.diff.Diffable;
import beam.core.diff.DiffableField;
import beam.core.diff.DiffableType;
import beam.core.diff.ResourceName;
import beam.lang.ast.KeyValueNode;
import beam.lang.ast.Node;
import beam.lang.ast.block.KeyBlockNode;
import beam.lang.ast.block.ResourceNode;
import beam.lang.ast.scope.ResourceScope;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.value.BooleanNode;
import beam.lang.ast.value.ListNode;
import beam.lang.ast.value.MapNode;
import beam.lang.ast.value.NumberNode;
import beam.lang.ast.value.StringNode;

public abstract class Resource implements Diffable {

    private String type;
    private String name;
    private ResourceScope scope;
    private Resource parent;

    // -- Internal

    private Set<Resource> dependencies;
    private Set<Resource> dependents;
    private Change change;

    // -- Resource Implementation API

    public abstract boolean refresh();

    public abstract void create();

    public abstract void update(Resource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

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

    public Change change() {
        return change;
    }

    public void change(Change change) {
        this.change = change;
    }

    public Set<Resource> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<Resource> dependents() {
        if (dependents == null) {
            dependents = new LinkedHashSet<>();
        }

        return dependents;
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

    public ResourceNode toResourceNode() {
        return new ResourceNode(
                resourceType(),
                new StringNode(resourceIdentifier()),
                toBodyNodes());
    }

    public List<Node> toBodyNodes() {
        List<Node> body = new ArrayList<>();

        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            Object value = field.getValue(this);

            if (value == null) {
                continue;
            }

            String key = field.getBeamName();

            if (value instanceof Boolean) {
                body.add(new KeyValueNode(key, new BooleanNode(Boolean.TRUE.equals(value))));

            } else if (value instanceof Number) {
                body.add(new KeyValueNode(key, new NumberNode((Number) value)));

            } else if (value instanceof List) {
                if (field.getSubresourceClass() != null) {
                    for (Object item : (List<?>) value) {
                        body.add(new KeyBlockNode(key, ((Resource) item).toBodyNodes()));
                    }

                } else {
                    body.add(new KeyValueNode(key, objectToNode(value)));
                }

            } else if (value instanceof Map) {
                body.add(new KeyValueNode(key, objectToNode(value)));

            } else if (value instanceof String) {
                body.add(new KeyValueNode(key, new StringNode((String) value)));

            } else if (value instanceof Resource) {
                body.add(new KeyBlockNode(key, ((Resource) value).toBodyNodes()));

            } else if (value instanceof Date) {
                body.add(new KeyValueNode(key, new StringNode(value.toString())));

            } else {
                throw new UnsupportedOperationException(String.format(
                        "Can't convert instance of [%s] in [%s] into a node!",
                        value.getClass().getName(),
                        name));
            }
        }

        return body;
    }

    @Override
    public String toString() {
        if (resourceIdentifier() == null) {
            return String.format("Resource[type: %s]", resourceType());
        }

        return String.format("Resource[type: %s, id: %s]", resourceType(), resourceIdentifier());
    }

    private Node objectToNode(Object value) {
        if (value instanceof Boolean) {
            return new BooleanNode(Boolean.TRUE.equals(value));

        } else if (value instanceof Number) {
            return new NumberNode((Number) value);

        } else if (value instanceof String) {
            return new StringNode((String) value);

        } else if (value instanceof Map) {
            Map map = (Map) value;
            List<KeyValueNode> entries = new ArrayList<>();

            for (Object key : map.keySet()) {
                Node valueNode = objectToNode(map.get(key));

                entries.add(new KeyValueNode((String) key, valueNode));
            }

            return new MapNode(entries);
        } else if (value instanceof List) {
            List<Node> items = new ArrayList<>();

            for (Object item : (List<?>) value) {
                items.add(objectToNode(item));
            }

            return new ListNode(items);
        } else {
            throw new UnsupportedOperationException(String.format(
                "Can't convert instance of [%s] in [%s] into a node!",
                value.getClass().getName(),
                name));
        }
    }

}
