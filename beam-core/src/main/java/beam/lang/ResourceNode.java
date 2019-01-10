package beam.lang;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;
import org.apache.commons.beanutils.BeanUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ResourceNode extends ContainerNode {

    private String type;
    private String name;
    private StringExpressionNode nameExpression;

    private Set<ResourceNode> dependencies;
    private Set<ResourceNode> dependents;
    private Map<String, List<ResourceNode>> subResources;

    public Set<ResourceNode> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<ResourceNode> dependents() {
        if (dependents == null) {
            dependents = new LinkedHashSet<>();
        }

        return dependents;
    }

    public Map<String, List<ResourceNode>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public void putSubresource(String fieldName, ResourceNode subresource) {
        List<ResourceNode> resources = subResources().computeIfAbsent(fieldName, r -> new ArrayList<>());
        resources.add(subresource);
    }

    public String resourceType() {
        return type;
    }

    public void setResourceType(String type) {
        this.type = type;
    }

    public String resourceIdentifier() {
        if (nameExpression != null) {
            return nameExpression.getValue();
        }

        return name;
    }

    public void setResourceIdentifier(String name) {
        this.name = name;
    }

    public void setResourceIdentifierExpression(StringExpressionNode nameExpression) {
        this.nameExpression = nameExpression;
    }

    public ResourceKey resourceKey() {
        return new ResourceKey(resourceType(), resourceIdentifier());
    }

    public ResourceNode parentResourceNode() {
        Node parent = parentNode();

        while (parent != null && !(parent instanceof ResourceNode)) {
            parent = parent.parentNode();
        }

        return (ResourceNode) parent;
    }

    public ResourceNode copy() {
        ResourceNode resource = (ResourceNode) super.copy();

        resource.setResourceType(resourceType());
        resource.setResourceIdentifier(resourceIdentifier());
        resource.syncPropertiesFromResource(this, true);

        // Copy subresources
        for (String fieldName : subResources().keySet()) {
            List<ResourceNode> subresources = new ArrayList<>();

            for (ResourceNode resourceNode : subResources().get(fieldName)) {
                subresources.add(resourceNode.copy());
            }

            resource.subResources().put(fieldName, subresources);
        }

        return resource;
    }

    protected final void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

                if (!BeanUtils.describe(this).containsKey(convertedKey)) {
                    ValueNode valueNode = get(key);
                    String message = String.format("invalid attribute '%s' found on line %s", key, valueNode.getLine());

                    throw new BeamException(message);
                }

                BeanUtils.setProperty(this, convertedKey, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                // Ignoring errors from setProperty
            }
        }

        for (String subResourceField : subResources().keySet()) {
            List<ResourceNode> subResources = subResources().get(subResourceField);

            try {
                BeanUtils.setProperty(this, subResourceField, subResources);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                // Ignoring errors from setProperty
                e.printStackTrace();
            }
        }
    }

    /**
     * Copy internal values from source to this object. This is used to copy information
     * from the current state (i.e. a resource loaded from a state file) into a pending
     * state (i.e. a resource loaded from a config file).
     */
    public void syncPropertiesFromResource(ResourceNode source) {
        syncPropertiesFromResource(source, false);
    }

    public void syncPropertiesFromResource(ResourceNode source, boolean force) {
        if (source == null) {
            return;
        }

        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {

                Method reader = p.getReadMethod();

                if (reader != null) {
                    Method writer = p.getWriteMethod();

                    ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
                    boolean isNullable = false;
                    if (propertyAnnotation != null) {
                        isNullable = propertyAnnotation.nullable();
                    }

                    Object currentValue = reader.invoke(source);
                    Object pendingValue = reader.invoke(this);

                    boolean isNullOrEmpty = pendingValue == null;
                    isNullOrEmpty = pendingValue instanceof Collection && ((Collection) pendingValue).isEmpty() ? true : isNullOrEmpty;

                    if (writer != null && (currentValue != null && isNullOrEmpty && (!isNullable || force))) {
                        writer.invoke(this, reader.invoke(source));
                    }
                }
            }

        } catch (IllegalAccessException | IntrospectionException error) {
            throw new IllegalStateException(error);
        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }
    }

    @Override
    public boolean resolve() {
        boolean resolved = super.resolve();

        if (nameExpression != null) {
            nameExpression.resolve();
        }

        for (List<ResourceNode> resources : subResources().values()) {
            for (ResourceNode resource : resources) {
                if (!resource.resolve()) {
                    throw new BeamLanguageException("Unable to resolve configuration.", resource);
                }
            }
        }

        if (resolved) {
            syncInternalToProperties();
        }

        return resolved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(resourceType()).append(" ");

        if (resourceIdentifier() != null) {
            sb.append('\'');
            sb.append(resourceIdentifier());
            sb.append('\'');
        }

        sb.append("\n");
        sb.append(super.toString());

        sb.append("end\n\n");

        return sb.toString();
    }

    /**
     * `execute()` is called during the parsing of the configuration. This
     * allows extensions to perform any necessary actions to load themselves.
     */
    public void execute() {

    }

    final void executeInternal() {
        syncInternalToProperties();
        execute();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResourceNode that = (ResourceNode) o;

        return Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

}
