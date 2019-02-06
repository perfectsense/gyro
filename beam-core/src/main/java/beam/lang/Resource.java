package beam.lang;

import beam.core.diff.Change;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceDisplayDiff;
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
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class Resource {

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

    public Map<String, Object> resolvedKeyValues() {
        Map<String, Object> copy = scope != null ? new HashMap<>(scope) : new HashMap<>();

        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null) {
                    String key = keyFromFieldName(p.getDisplayName());
                    Object value = reader.invoke(this);

                    copy.put(key, value);
                }
            }
        } catch (IllegalAccessException | IntrospectionException error) {
            throw new IllegalStateException(error);
        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }

        return copy;
    }

    public ResourceDisplayDiff calculateFieldDiffs(Resource current) {
        boolean firstField = true;

        ResourceDisplayDiff displayDiff = new ResourceDisplayDiff();

        Map<String, Object> currentValues = current.resolvedKeyValues();
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : pendingValues.keySet()) {
            // If there is no getter for this method then skip this field since there can
            // be no ResourceDiffProperty annotation.
            Method reader = readerMethodForKey(key);
            if (reader == null) {
                continue;
            }

            // If no ResourceDiffProperty annotation or if this field has subresources then skip this field.
            ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
            if (propertyAnnotation == null || propertyAnnotation.subresource()) {
                continue;
            }
            boolean nullable = propertyAnnotation.nullable();

            Object currentValue = currentValues.get(key);
            Object pendingValue = pendingValues.get(key);

            if (pendingValue != null || nullable) {
                String fieldChangeOutput = null;
                if (pendingValue instanceof List) {
                    fieldChangeOutput = Change.processAsListValue(key, (List) currentValue, (List) pendingValue);
                } else if (pendingValue instanceof Map) {
                    fieldChangeOutput = Change.processAsMapValue(key, (Map) currentValue, (Map) pendingValue);
                } else {
                    fieldChangeOutput = Change.processAsScalarValue(key, currentValue, pendingValue);
                }

                if (!ObjectUtils.isBlank(fieldChangeOutput)) {
                    if (!firstField) {
                        displayDiff.getChangedDisplay().append(", ");
                    }

                    displayDiff.addChangedProperty(key);
                    displayDiff.getChangedDisplay().append(fieldChangeOutput);

                    if (!propertyAnnotation.updatable()) {
                        displayDiff.setReplace(true);
                    }

                    firstField = false;
                }
            }
        }

        return displayDiff;
    }

    // -- Base Resource

    public Object get(String key) {
        return Optional.ofNullable(ResourceType.getInstance(getClass()).getFieldByBeamName(key))
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
        for (ResourceField field : ResourceType.getInstance(getClass()).getFields()) {
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

        for (ResourceField field : ResourceType.getInstance(getClass()).getFields()) {
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

    String fieldNameFromKey(String key) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
    }

    String keyFromFieldName(String field) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field).replaceFirst("^get-", "");
    }

    Method readerMethodForKey(String key) {
        PropertyDescriptor p = propertyDescriptorForKey(key);
        if (p != null) {
            return p.getReadMethod();
        }

        return null;
    }

    Method writerMethodForKey(String key) {
        PropertyDescriptor p = propertyDescriptorForKey(key);
        if (p != null) {
            return p.getWriteMethod();
        }

        return null;
    }

    PropertyDescriptor propertyDescriptorForKey(String key) {
        String convertedKey = fieldNameFromKey(key);
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                if (p.getDisplayName().equals(convertedKey)) {
                    return p;
                }
            }
        } catch (IntrospectionException ex) {
            // Ignoring introspection exceptions
        }

        return null;
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
