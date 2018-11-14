package beam.core;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beam.core.diff.ResourceChange;

import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;
import beam.lang.BeamResolvable;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

public abstract class BeamResource<C extends BeamCloud> extends BeamConfig implements Comparable<BeamResource> {

    private String resourceName;
    private transient BeamResource<C> parent;
    private transient List<BeamResource<C>> children = new ArrayList<>();
    private transient final Map<String, BeamResource<C>> dependencies = new TreeMap<>();
    private transient final List<BeamResource<C>> dependents = new ArrayList<>();
    private transient ResourceChange change;

    @Override
    public boolean resolve(BeamConfig config) {
        boolean progress = super.resolve(config);

        for (BeamConfigKey key : getContext().keySet()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getContext().get(key);
            Object value = referable.getValue();

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

    public Map<String, BeamResource<C>> dependencies() {
        return dependencies;
    }

    public List<BeamResource<C>> dependents() {
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

    public void resolveDependencies() {
        for (String field : dependencies.keySet())  {
            BeamResource parent = dependencies.get(field);

            try {
                FieldUtils.writeDeclaredField(this, field, parent, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void calculateDependencies(BeamResource resource, List<BeamResource> resources) {
        Pattern var = Pattern.compile("\\$\\{([^.]+)\\}");

        try {
            Class<?> objectClass = resource.getClass();

            for (PropertyDescriptor p : Introspector.getBeanInfo(objectClass).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null) {
                    Object value = reader.invoke(resource);

                    if (value != null) {
                        // Loop over values (or single value if scalar)
                        for (Object item : ObjectUtils.to(Iterable.class, value)) {
                            if (item instanceof String) {
                                String stringValue = (String) item;

                                Matcher match = var.matcher(stringValue);

                                if (match.find()) {
                                    String resourceName = match.group(1);
                                    BeamResource parent = null;

                                    for (BeamResource r : resources) {
                                        if (resourceName.equals(r.getResourceName())) {
                                            parent = r;
                                            break;
                                        }
                                    }

                                    if (parent == null) {
                                        String error = String.format("Unable to resolve dependency '%s: %s' on resource %s ",
                                                p.getName(), stringValue, resource.getResourceName());
                                        throw new BeamException(error);
                                    }

                                    resource.parent = parent;
                                    parent.children.add(resource);

                                    resource.dependencies.put(p.getName(), parent);
                                    parent.dependents.add(resource);
                                }
                            }
                        }
                    }
                }
            }

        } catch (IllegalAccessException |
                IntrospectionException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error.getCause());
        }
    }

}
