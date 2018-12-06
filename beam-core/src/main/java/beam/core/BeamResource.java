package beam.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import beam.core.diff.ResourceChange;

import beam.core.diff.ResourceName;
import beam.lang.BeamConfig;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamReference;
import beam.lang.BeamResolvable;
import beam.lang.BeamList;
import beam.lang.BeamLiteral;
import beam.lang.BeamScalar;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

public abstract class BeamResource extends BeamConfig implements Comparable<BeamResource> {

    private String resourceIdentifier;
    private BeamCredentials resourceCredentials;
    private String path;

    private transient BeamResource parent;
    private transient List<BeamResource> children = new ArrayList<>();
    private transient final Set<BeamResource> dependencies = new TreeSet<>();
    private transient final Set<BeamResource> dependents = new TreeSet<>();
    private transient ResourceChange change;
    private transient BeamConfig root;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean resolve(BeamContext context) {
        String id = getParams().get(0).getValue().toString();
        setResourceIdentifier(id);

        if (get("resource-credentials") == null) {
            BeamContextKey credentialsKey = new BeamContextKey("default", getResourceCredentialsName());

            BeamReference credentialsReference = new BeamReference();
            credentialsReference.getScopeChain().add(credentialsKey);

            // Add reference to current resource
            BeamContextKey resourceCredentialsKey = new BeamContextKey("resource-credentials");
            addReferable(resourceCredentialsKey, credentialsReference);
        }

        boolean progress = super.resolve(context);
        for (BeamContextKey key : listContextKeys()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getReferable(key);
            Object value = referable.getValue();

            try {
                String keyId = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key.getId());
                BeanUtils.setProperty(this, keyId, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {

            }
        }

        return progress;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamConfig config) {
        Set<BeamReference> dependencies = super.getDependencies(config);
        BeamResolvable resolvable = get("depends-on");
        if (resolvable instanceof BeamList) {
            BeamList beamList = (BeamList) resolvable;
            for (BeamScalar beamScalar : beamList.getList()) {
                if (beamScalar.getElements().size() != 1) {
                    throw new IllegalStateException();
                }

                BeamLiteral beamLiteral = beamScalar.getElements().get(0);
                if (beamLiteral instanceof BeamReference) {
                    dependencies.add((BeamReference) beamLiteral);
                } else {
                    throw new IllegalArgumentException("depends-on contains non reference value");
                }
            }
        } else if (resolvable != null) {
            throw new IllegalArgumentException("depends-on has to be a list");
        }

        this.dependencies.clear();
        for (BeamReference reference : dependencies) {
            Object dependency = reference.getValue();
            if (dependency instanceof BeamResource) {
                BeamResource resource = (BeamResource) dependency;
                this.dependencies.add(resource);
                resource.dependents.add(this);

            } else {
                throw new BeamException("Dependency has to be BeamResource");
            }
        }

        return dependencies;
    }

    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public abstract Class getResourceCredentialsClass();

    public String getResourceCredentialsName() {
        Class c = getResourceCredentialsClass();

        try {
            BeamCredentials credentials = (BeamCredentials) c.newInstance();

            String resourceNamespace = credentials.getName();
            String resourceName = c.getSimpleName();
            if (c.isAnnotationPresent(ResourceName.class)) {
                ResourceName name = (ResourceName) c.getAnnotation(ResourceName.class);
                resourceName = name.value();

                return String.format("%s::%s", resourceNamespace, resourceName);
            }
        } catch (Exception ex) {
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

    public Set<BeamResource> dependencies() {
        return dependencies;
    }

    public Set<BeamResource> dependents() {
        return dependents;
    }

    public BeamConfig getRoot() {
        return root;
    }

    public void setRoot(BeamConfig root) {
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
        for (BeamResource parent = this; (parent = parent.parent) != null;) {
            if (parentClass.isInstance(parent)) {
                return (T) parent;
            }
        }

        return null;
    }

    public abstract void refresh();

    public BeamResource findCurrent() {
        return null;
    }

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
