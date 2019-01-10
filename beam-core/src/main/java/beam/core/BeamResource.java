package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceDisplayDiff;
import beam.core.diff.ResourceName;
import beam.lang.BeamLanguageException;
import beam.lang.Container;
import beam.lang.Node;
import beam.lang.ReferenceNode;
import beam.lang.ResourceKey;
import beam.lang.StringExpressionNode;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
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

public abstract class BeamResource extends Container implements Comparable<BeamResource> {

    private String type;
    private String name;
    private StringExpressionNode nameExpression;
    private Set<BeamResource> dependencies;
    private Set<BeamResource> dependents;
    private Map<String, List<BeamResource>> subResources;

    private transient BeamCredentials resourceCredentials;
    private transient ResourceChange change;

    public abstract boolean refresh();

    public abstract void create();

    public abstract void update(BeamResource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

    public abstract Class resourceCredentialsClass();

    @Override
    public BeamResource copy() {
        BeamResource resource = (BeamResource) super.copy();
        resource.setResourceCredentials(getResourceCredentials());
        resource.setResourceType(resourceType());
        resource.setResourceIdentifier(resourceIdentifier());
        resource.syncPropertiesFromResource(this, true);

        // Copy subresources
        for (String fieldName : subResources().keySet()) {
            List<BeamResource> subresources = new ArrayList<>();

            for (BeamResource subresource : subResources().get(fieldName)) {
                subresources.add(subresource.copy());
            }

            resource.subResources().put(fieldName, subresources);
        }

        return resource;
    }

    public String resourceCredentialsName() {
        Class c = resourceCredentialsClass();

        try {
            BeamCredentials credentials = (BeamCredentials) c.newInstance();

            String resourceNamespace = credentials.getCloudName();
            String resourceName = c.getSimpleName();
            if (c.isAnnotationPresent(ResourceName.class)) {
                ResourceName name = (ResourceName) c.getAnnotation(ResourceName.class);
                resourceName = name.value();

                return String.format("%s::%s", resourceNamespace, resourceName);
            }
        } catch (Exception ex) {
            throw new BeamException("Unable to determine credentials resource name.", ex);
        }

        return c.getSimpleName();
    }

    public String primaryKey() {
        return String.format("%s %s", resourceType(), resourceIdentifier());
    }

    public BeamCredentials getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(BeamCredentials resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

    public ResourceChange change() {
        return change;
    }

    public void setChange(ResourceChange change) {
        this.change = change;
    }

    public void diffOnCreate(ResourceChange change) throws Exception {
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.create((List) pendingValue);
            } else {
                change.createOne((BeamResource) pendingValue);
            }
        }
    }

    public void diffOnUpdate(ResourceChange change, BeamResource current) throws Exception {
        Map<String, Object> currentValues = current.resolvedKeyValues();
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {

            Object currentValue = currentValues.get(key);
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.update((List) currentValue, (List) pendingValue);
            } else {
                change.updateOne((BeamResource) currentValue, (BeamResource) pendingValue);
            }
        }
    }

    public void diffOnDelete(ResourceChange change) throws Exception {
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.delete((List) pendingValue);
            } else {
                change.deleteOne((BeamResource) pendingValue);
            }
        }
    }

    public ResourceDisplayDiff calculateFieldDiffs(BeamResource current) {
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
                    fieldChangeOutput = ResourceChange.processAsListValue(key, (List) currentValue, (List) pendingValue);
                } else if (pendingValue instanceof Map) {
                    fieldChangeOutput = ResourceChange.processAsMapValue(key, (Map) currentValue, (Map) pendingValue);
                } else {
                    fieldChangeOutput = ResourceChange.processAsScalarValue(key, currentValue, pendingValue);
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

    public void execute() {
        if (get("resource-credentials") == null) {
            ReferenceNode credentialsReference = new ReferenceNode(resourceCredentialsName(), "default");
            credentialsReference.setLine(getLine());
            credentialsReference.setColumn(getColumn());

            put("resource-credentials", credentialsReference);
        }
    }

    public final void executeInternal() {
        syncInternalToProperties();
        execute();
    }

    @Override
    public int compareTo(BeamResource o) {
        if (o == null) {
            return 1;
        }

        String compareKey = String.format("%s %s", resourceType(), resourceIdentifier());
        String otherKey = String.format("%s %s", o.resourceType(), o.resourceIdentifier());

        return compareKey.compareTo(otherKey);
    }

    /**
     * Return a list of fields that contain subresources (ResourceDiffProperty(subresource = true)).
     */
    private List<String> subresourceFields() {
        List<String> keys = new ArrayList<>();
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : pendingValues.keySet()) {
            // If there is no getter for this method then skip this field since there can
            // be no ResourceDiffProperty annotation.
            Method reader = readerMethodForKey(key);
            if (reader == null) {
                continue;
            }

            // If no ResourceDiffProperty annotation or if this field is not subresources then skip this field.
            ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
            if (propertyAnnotation == null || !propertyAnnotation.subresource()) {
                continue;
            }

            keys.add(key);
        }

        return keys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BeamResource that = (BeamResource) o;

        return Objects.equals(primaryKey(), that.primaryKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryKey());
    }

    public Set<BeamResource> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<BeamResource> dependents() {
        if (dependents == null) {
            dependents = new LinkedHashSet<>();
        }

        return dependents;
    }

    public Map<String, List<BeamResource>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public void putSubresource(String fieldName, BeamResource subresource) {
        List<BeamResource> resources = subResources().computeIfAbsent(fieldName, r -> new ArrayList<>());
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

    public BeamResource parentResourceNode() {
        Node parent = parentNode();

        while (parent != null && !(parent instanceof BeamResource)) {
            parent = parent.parentNode();
        }

        return (BeamResource) parent;
    }

    protected final void syncInternalToProperties() {
        super.syncInternalToProperties();

        for (String subResourceField : subResources().keySet()) {
            List<BeamResource> subResources = subResources().get(subResourceField);

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
    public void syncPropertiesFromResource(BeamResource source) {
        syncPropertiesFromResource(source, false);
    }

    public void syncPropertiesFromResource(BeamResource source, boolean force) {
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

        for (List<BeamResource> resources : subResources().values()) {
            for (BeamResource resource : resources) {
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
}
