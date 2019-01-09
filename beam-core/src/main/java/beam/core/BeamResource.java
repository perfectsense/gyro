package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceDisplayDiff;
import beam.core.diff.ResourceName;
import beam.lang.BeamBaseResource;
import beam.lang.ReferenceNode;
import beam.lang.ResourceNode;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class BeamResource extends BeamBaseResource implements Comparable<BeamResource>, Cloneable {

    private transient BeamCredentials resourceCredentials;

    private transient ResourceChange change;

    public abstract boolean refresh();

    public abstract void create();

    public abstract void update(BeamResource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

    public abstract Class resourceCredentialsClass();

    public BeamResource copy() {
        try {
            BeamResource resource = getClass().newInstance();

            resource.setResourceType(resourceType());
            resource.setResourceIdentifier(resourceIdentifier());
            resource.setResourceCredentials(getResourceCredentials());
            resource.syncPropertiesFromResource(this, true);

            // Copy subresources
            for (String fieldName : subResources().keySet()) {
                List<ResourceNode> subresources = new ArrayList<>();

                for (ResourceNode resourceNode : subResources().get(fieldName)) {
                    if (resourceNode instanceof BeamResource) {
                        BeamResource beamResource = (BeamResource) resourceNode;
                        subresources.add(beamResource.copy());
                    }
                }

                resource.subResources().put(fieldName, subresources);
            }

            return resource;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeamException("");
        }
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
            throw new BeamException("Unable to determine credentials resource name.");
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

    public void diffOnCreate(ResourceChange change) throws Exception {
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.create((Collection) pendingValue);
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
                change.update((Collection) currentValue, (Collection) pendingValue);
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
                change.delete((Collection) pendingValue);
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

    @Override
    public void execute() {
        if (get("resource-credentials") == null) {
            ReferenceNode credentialsReference = new ReferenceNode(resourceCredentialsName(), "default");
            credentialsReference.setLine(getLine());
            credentialsReference.setColumn(getColumn());

            put("resource-credentials", credentialsReference);
        }
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

}
