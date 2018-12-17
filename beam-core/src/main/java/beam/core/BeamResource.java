package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceName;
import beam.lang.BeamLanguageExtension;
import beam.lang.types.BeamBlock;
import beam.lang.types.BeamReference;
import beam.lang.types.KeyValueBlock;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class BeamResource extends BeamLanguageExtension implements Comparable<BeamResource> {

    private BeamCredentials resourceCredentials;
    private String path;
    private List<BeamResource> dependsOn;

    private transient BeamResource parent;
    private transient List<BeamResource> children = new ArrayList<>();
    private final transient Set<BeamResource> dependencies = new TreeSet<>();
    private final transient Set<BeamResource> dependents = new TreeSet<>();
    private transient ResourceChange change;
    private transient BeamBlock root;

    public List<BeamResource> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<BeamResource> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public Set<BeamResource> resourceDependencies() {
        return dependencies;
    }

    public Set<BeamResource> resourceDependents() {
        return dependents;
    }

    public BeamBlock getRoot() {
        return root;
    }

    public void setRoot(BeamBlock root) {
        this.root = root;
    }

    public BeamResource findTop() {
        BeamResource top = this;

        while (true) {
            BeamResource parent = top.parent;

            if (parent == null) {
                return top;

            } else {
                top = parent;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BeamResource> T findParent(Class<T> parentClass) {
        for (BeamResource parent = this; (parent = parent.parent) != null; ) {
            if (parentClass.isInstance(parent)) {
                return (T) parent;
            }
        }

        return null;
    }

    public BeamResource findCurrent() {
        return null;
    }

    @Override
    public void execute() {
        if (get("resource-credentials") == null) {
            BeamReference credentialsReference = new BeamReference(getResourceCredentialsName(), "default");
            credentialsReference.setParentBlock(getParentBlock());

            KeyValueBlock credentialsBlock = new KeyValueBlock();
            credentialsBlock.setKey("resource-credentials");
            credentialsBlock.setValue(credentialsReference);

            getBlocks().add(credentialsBlock);
        }
    }

    public abstract void refresh();

    public abstract void create();

    public abstract void update(BeamResource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

    @Override
    public int compareTo(BeamResource o) {
        if (getResourceIdentifier() != null) {
            return getResourceIdentifier().compareTo(o.getResourceIdentifier());
        }

        return -1;
    }

}
