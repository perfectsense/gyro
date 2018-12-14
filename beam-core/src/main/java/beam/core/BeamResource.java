package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceName;
import beam.lang.BeamBlock;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamInterp;
import beam.lang.types.BeamList;
import beam.lang.BeamLiteral;
import beam.lang.BeamReference;
import beam.lang.BeamResolvable;
import beam.lang.types.BeamScalar;
import com.google.common.base.CaseFormat;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class BeamResource extends BeamValidatedBlock implements Comparable<BeamResource> {

    private String resourceIdentifier;
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
            add(resourceCredentialsKey, credentialsReference);
        }

        boolean progress = super.resolve(context);
        populate();

        return progress;
    }

    @Override
    public void applyExtension(BeamInterp interp) {
        List<BeamBlock> newConfigs = new ArrayList<>();
        Iterator<BeamBlock> iterator = getChildren().iterator();
        while (iterator.hasNext()) {
            BeamBlock config = iterator.next();

            Class<? extends BeamBlock> extension = null;
            if (interp.hasExtension(config.getType())) {
                extension = interp.getExtension(config.getType());
            } else {
                try {
                    String keyId = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, config.getType());
                    PropertyDescriptor pd = new PropertyDescriptor(keyId, getClass());
                    Method setter = pd.getWriteMethod();

                    if (setter != null && setter.getParameterTypes().length == 1) {
                        Class parameterType = setter.getParameterTypes()[0];
                        Type[] types = setter.getGenericParameterTypes();
                        Class type = parameterType;

                        if (Collection.class.isAssignableFrom(parameterType)) {
                            ParameterizedType paramType = (ParameterizedType) types[0];
                            type = (Class<?>) paramType.getActualTypeArguments()[0];
                            BeamContextKey key = new BeamContextKey(config.getType());
                            if (containsKey(key) && !(get(key) instanceof BeamList)) {
                                throw new BeamException(String.format("Expect %s in %s to be a BeamList, found %s",
                                    key, getClass(), get(key).getClass()));
                            } else {
                                BeamList beamList = new BeamList();
                                add(key, beamList);
                            }
                        }

                        if (interp.hasExtension(type.getName())) {
                            extension = interp.getExtension(type.getName());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (extension == null) {
                continue;
            }

            if (config.getClass() != extension) {
                BeamBlock newConfig = interp.createConfig(config.getType(), config);
                newConfig.applyExtension(interp);

                newConfigs.add(newConfig);

                iterator.remove();
            } else {
                config.applyExtension(interp);
            }
        }

        getChildren().addAll(newConfigs);
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
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

    public Set<BeamResource> dependencies() {
        return dependencies;
    }

    public Set<BeamResource> dependents() {
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
