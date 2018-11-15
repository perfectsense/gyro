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

public abstract class BeamResource<C extends BeamCloud> extends BeamConfig implements Comparable<BeamResource> {

    private String resourceName;
    private transient BeamResource<C> parent;
    private transient List<BeamResource<C>> children = new ArrayList<>();
    private transient final Set<BeamResource<C>> dependencies = new TreeSet<>();
    private transient final Set<BeamResource<C>> dependents = new TreeSet<>();
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
                BeamResource parent = (BeamResource) value;

                parent.dependents.add(this);
                dependencies.add(parent);
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

    public ResourceChange getChange() {
        return change;
    }

    public void setChange(ResourceChange change) {
        this.change = change;
    }

    public Set<BeamResource<C>> dependencies() {
        return dependencies;
    }

    public Set<BeamResource<C>> dependents() {
        return dependents;
    }

    public BeamResource<C> findTop() {
        BeamResource<C> top = this;

        while (true) {
            BeamResource<C> parent = top.parent;

            if (parent == null) {
                return top;

            } else {
                top = parent;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BeamResource<C>> T findParent(Class<T> parentClass) {
        for (BeamResource<C> parent = this; (parent = parent.parent) != null;) {
            if (parentClass.isInstance(parent)) {
                return (T) parent;
            }
        }

        return null;
    }

    public abstract void refresh(C cloud);

    public BeamResource<C> findCurrent(C cloud) {
        return null;
    }

    public abstract void create(C cloud);

    public abstract void update(C cloud, BeamResource<C> current, Set<String> changedProperties);

    public abstract void delete(C cloud);

    public abstract String toDisplayString();

    @Override
    public int compareTo(BeamResource o) {
        if (getResourceName() != null) {
            return getResourceName().compareTo(o.getResourceName());
        }

        return -1;
    }

}
