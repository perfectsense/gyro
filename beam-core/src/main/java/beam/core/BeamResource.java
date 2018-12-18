package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceName;
import beam.lang.BeamLanguageExtension;
import beam.lang.types.BeamReference;
import beam.lang.types.KeyValueBlock;
import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.beanutils.BeanUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public abstract class BeamResource extends BeamLanguageExtension implements Comparable<BeamResource> {

    private BeamCredentials resourceCredentials;
    private List<BeamResource> dependsOn;

    private transient ResourceChange change;

    public abstract void refresh();

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

    public void sync() {
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null && reader.getReturnType() == String.class) {
                    String key = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, reader.getName().substring(3));
                    String value = (String) reader.invoke(this);

                    set(key, value);
                }
            }

        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
            // Ignoring exceptions
        }
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

            getBlocks().add(credentialsBlock);
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
