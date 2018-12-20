package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.BeamLanguageExtension;
import beam.lang.types.BeamReference;
import beam.lang.types.KeyValueBlock;
import beam.lang.types.ResourceBlock;
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;
import org.apache.commons.beanutils.BeanUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BeamResource extends BeamLanguageExtension implements Comparable<BeamResource> {

    private BeamCredentials resourceCredentials;
    private List<BeamResource> dependsOn;

    private transient ResourceChange change;

    public abstract boolean refresh();

    public abstract void create();

    public abstract void update(BeamResource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

    public List<BeamResource> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<BeamResource> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public abstract Class getResourceCredentialsClass();

    public String getResourceCredentialsName() {
        Class c = getResourceCredentialsClass();

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

    public BeamCredentials getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(BeamCredentials resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

    public ResourceChange getChange() {
        return change;
    }

    public void setChange(ResourceChange change) {
        this.change = change;
    }

    /**
     * Copy values from properties into internal values. This is used to ensure updates to resource
     * properties from executing (i.e. creating/updating a resource) are in the internal state.
     */
    public void syncPropertiesToInternal() {
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null
                    && (reader.getReturnType() == String.class || reader.getReturnType() == List.class || reader.getReturnType() == Map.class)) {
                    String key = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, p.getDisplayName());
                    Object value = reader.invoke(this);

                    if (value instanceof String) {
                        put(key, (String) value);
                    } else if (value instanceof List) {
                        putList(key, (List) value);
                    } else if (value instanceof Map) {
                        putMap(key, (Map) value);
                    }
                }
            }

        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
            // Ignoring exceptions
        }
    }

    /**
     * Copy internal values from source to this object. This is used to copy information
     * from the current state (i.e. a resource loaded from a state file) into a pending
     * state (i.e. a resource loaded from a config file).
     */
    public void syncPropertiesFromResource(ResourceBlock source) {
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

                    if (writer != null && (currentValue != null && pendingValue == null && !isNullable)) {
                        writer.invoke(this, reader.invoke(source));
                    }
                }
            }

        } catch (IllegalAccessException | IntrospectionException error) {
            throw new IllegalStateException(error);
        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }

        syncPropertiesToInternal();
    }

    @Override
    public void execute() {
        if (get("resource-credentials") == null) {
            BeamReference credentialsReference = new BeamReference(getResourceCredentialsName(), "default");
            credentialsReference.setParentBlock(getParentBlock());

            KeyValueBlock credentialsBlock = new KeyValueBlock();
            credentialsBlock.setParentBlock(this);
            credentialsBlock.setKey("resource-credentials");
            credentialsBlock.setValue(credentialsReference);

            putKeyValue(credentialsBlock);
        }
    }

    @Override
    public int compareTo(BeamResource o) {
        if (o == null) {
            return 1;
        }

        String compareKey = String.format("%s %s", getResourceType(), getResourceIdentifier());
        String otherKey = String.format("%s %s", o.getResourceType(), o.getResourceIdentifier());

        return compareKey.compareTo(otherKey);
    }

}
