package beam.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import beam.core.diff.ResourceChange;

import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;
import beam.lang.BeamResolvable;
import org.apache.commons.beanutils.BeanUtils;

public abstract class BeamResource extends BeamConfig implements Comparable<BeamResource> {

    private String resourceName;
    private BeamCredentials resourceCredentials;

    private transient BeamResource parent;
    private transient List<BeamResource> children = new ArrayList<>();
    private transient final Set<BeamResource> dependencies = new TreeSet<>();
    private transient final Set<BeamResource> dependents = new TreeSet<>();
    private transient ResourceChange change;

    @Override
    public boolean resolve(BeamConfig config) {
        boolean progress = super.resolve(config);

        if (!progress) {
            return progress;
        }

        for (BeamConfigKey key : getContext().keySet()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getContext().get(key);
            Object value = referable.getValue();

            if (value instanceof BeamResource) {
                parent = (BeamResource) value;
                parent.dependents.add(this);
                dependencies.add(parent);
            } else if (value instanceof BeamCredentials) {
                BeamCredentials credentials = (BeamCredentials) value;
                credentials.dependents().add(this);
            }

            try {
                BeanUtils.setProperty(this, key.getId(), value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {

            }
        }

        return progress;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public abstract Class getResourceCredentialsClass();

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

    public Set<BeamResource> dependencies() {
        return dependencies;
    }

    public Set<BeamResource> dependents() {
        return dependents;
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
        for (BeamResource parent = this; (parent = parent.parent) != null;) {
            if (parentClass.isInstance(parent)) {
                return (T) parent;
            }
        }

        return null;
    }

    public abstract void refresh(BeamCredentials cloud);

    public BeamResource findCurrent() {
        return null;
    }

    public abstract void create();

    public abstract void update(BeamResource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

    @Override
    public int compareTo(BeamResource o) {
        if (getResourceName() != null) {
            return getResourceName().compareTo(o.getResourceName());
        }

        return -1;
    }

}
