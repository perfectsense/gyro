package beam.core;

import beam.core.diff.ResourceDiffProperty;
import beam.parser.ASTHandler;
import beam.parser.BeamConfigGenerator;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import beam.core.diff.ResourceChange;

public abstract class BeamResource<B extends BeamProvider> extends BeamObject {

    private BeamResource<B> parent;
    private List<BeamResource<B>> children = new ArrayList<>();
    private final Set<BeamResource<B>> dependencies = new HashSet<>();
    private final Set<BeamResource<B>> dependents = new HashSet<>();
    private String beamId;
    private ResourceChange change;
    private Map<String, BeamReference> references = new HashMap<>();
    private Map<String, Object> unResolvedProperties = new ConcurrentHashMap<>();
    private BeamContext context;

    /**
     * Returns {@code true} if the given {@code awsResource} should be
     * included in the list of current resources according to the given
     * {@code filter}.
     *
     * @param filter May be {@code null}.
     * @param awsResource May be {@code null}.
     */
    public static boolean isInclude(BeamResourceFilter filter, Object awsResource) {
        return filter == null || filter.isInclude(awsResource);
    }

    /**
     * Updates the parent/child relationships on all resources in the
     * given {@code cloudConfig}.
     *
     * @param providerConfig Can't be {@code null}.
     */
    public static void updateTree(ProviderConfig providerConfig) {
        updateTreeRecursively(providerConfig, null);
        updateDependenciesRecursively(providerConfig);
    }

    private static void updateTreeRecursively(Object object, BeamResource<? extends BeamProvider> parent) {
        try {
            Class<?> objectClass = object.getClass();

            for (PropertyDescriptor p : Introspector.getBeanInfo(objectClass).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null) {
                    Object value = reader.invoke(object);

                    if (value != null) {
                        for (Object item : ObjectUtils.to(Iterable.class, value)) {
                            if (item instanceof BeamResource) {
                                BeamResource itemResource = (BeamResource) item;

                                if (parent != null) {
                                    itemResource.parent = parent;
                                    parent.children.add(itemResource);
                                }

                                updateTreeRecursively(item, itemResource);
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

    private static void updateDependenciesRecursively(Object object) {
        try {
            Class<?> objectClass = object.getClass();

            for (PropertyDescriptor p : Introspector.getBeanInfo(objectClass).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null) {
                    Object value = reader.invoke(object);

                    if (value != null) {
                        for (Object item : ObjectUtils.to(Iterable.class, value)) {
                            if (item instanceof BeamResource) {
                                updateDependenciesRecursively(item);

                            } else if (object instanceof BeamResource &&
                                    item instanceof BeamReference) {

                                BeamReference itemRef = (BeamReference) item;
                                BeamResource<? extends BeamProvider> itemResource = itemRef.resolve();

                                if (itemResource != null) {
                                    BeamResource objectResource = (BeamResource) object;

                                    objectResource.dependencies.add(itemResource);
                                    itemResource.dependents.add(objectResource);
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

    public static Set<String> awsIdSet(Iterable<? extends BeamReference> references) {
        Set<String> awsIds = new HashSet<>();

        if (references != null) {
            for (BeamReference r : references) {
                String awsId = r.awsId();

                if (awsId != null) {
                    awsIds.add(awsId);
                }
            }
        }

        return awsIds;
    }

    public String getBeamId() {
        return beamId;
    }

    public void setBeamId(String beamId) {
        this.beamId = beamId;
    }

    public ResourceChange getChange() {
        return change;
    }

    public void setChange(ResourceChange change) {
        this.change = change;
    }

    public BeamResource<B> getParent() {
        return parent;
    }

    public void setParent(BeamResource<B> parent) {
        this.parent = parent;
    }

    public List<BeamResource<B>> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }

        return children;
    }

    public void setChildren(List<BeamResource<B>> children) {
        this.children = children;
    }

    public Set<BeamResource<B>> dependencies() {
        return dependencies;
    }

    public Set<BeamResource<B>> dependents() {
        return dependents;
    }

    /**
     * Creates a new reference based on the given {@code resource}.
     *
     * @param resource If {@code null}, returns {@code null}.
     * @return May be {@code null}.
     */
    public BeamReference newReference(BeamResource<B> resource) {
        return resource != null ? new BeamReference(this, resource) : null;
    }

    /**
     * Creates a new referenced based on the given {@code awsId}.
     *
     * @param awsId If {@code null}, returns {@code null}.
     * @return May be {@code null}.
     */
    public BeamReference newReference(Class<? extends BeamResource<B>> resourceClass, String awsId) {
        return awsId != null ? new BeamReference(this, resourceClass, awsId) : null;
    }

    /**
     * Creates a new set of references based on the given {@code awsIds}.
     *
     * @param awsIds If {@code null}, returns an empty set.
     * @return Never {@code null}.
     */
    public Set<BeamReference> newReferenceSet(Class<? extends BeamResource<B>> resourceClass, Iterable<String> awsIds) {
        Set<BeamReference> refs = new HashSet<>();

        if (awsIds != null) {
            for (String awsId : awsIds) {
                refs.add(newReference(resourceClass, awsId));
            }
        }

        return refs;
    }

    /**
     * Returns a reference to the parent that's an instance of the given
     * {@code resourceClass}, or if not found, the given
     * {@code defaultReference}.
     *
     * @param resourceClass Can't be {@code null}.
     * @param defaultReference May be {@code null}.
     * @return May be {@code null}.
     */
    public BeamReference newParentReference(Class<? extends BeamResource<B>> resourceClass, BeamReference defaultReference) {
        BeamResource<B> parent = findParent(resourceClass);

        return parent != null ? newReference(parent) : defaultReference;
    }

    @SuppressWarnings("unchecked")
    public <T extends BeamResource<B>> T findParent(Class<T> parentClass) {
        for (BeamResource<B> parent = this; (parent = parent.parent) != null;) {
            if (parentClass.isInstance(parent)) {
                return (T) parent;
            }
        }

        return null;
    }

    /**
     * Finds a resource of the given {@code resourceClass} with the given
     * {@code id}.
     *
     * @param id If {@code null}, always returns {@code null}.
     */
    public <R extends BeamResource<B>> R findById(Class<R> resourceClass, String id) {
        return id != null ? findTop().findByIdRecursively(resourceClass, id) : null;
    }

    public BeamResource<B> findTop() {
        BeamResource<B> top = this;

        while (true) {
            BeamResource<B> parent = top.parent;

            if (parent == null) {
                return top;

            } else {
                top = parent;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <R extends BeamResource<B>> R findByIdRecursively(Class<R> resourceClass, String id) {
        if (resourceClass.isInstance(this)) {
            R resource = (R) this;

            if (id.equals(resource.awsId()) ||
                    id.equals(resource.getBeamId())) {

                return resource;
            }
        }

        for (BeamResource<B> child : children) {
            R found = child.findByIdRecursively(resourceClass, id);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Returns the ID used within AWS.
     *
     * @return May be {@code null}.
     */
    public String awsId() {
        return null;
    }

    public abstract List<?> diffIds();

    public abstract void init(B cloud, BeamResourceFilter filter, Set<BeamResource<B>> resourceSet);

    public void initAsync(List<CompletableFuture> futures, B cloud, BeamResourceFilter filter, Set<BeamResource<B>> resourceSet) {
        CompletableFuture future = CompletableFuture.supplyAsync(
                () -> {
                    init(cloud, filter, resourceSet);
                    return this;
                }
        );

        futures.add(future);
    }

    public static List pollFutures(List<CompletableFuture> futures) {
        List values = new ArrayList<>();

        for (CompletableFuture future : futures) {
            while (!future.isDone()) {
                try {
                    values.add(future.get());
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return values;
    }

    private Map<String, List<BeamResource>> getResourcesByType(List<BeamResource<B>> children) {
        Map<String, List<BeamResource>> resourceByType = new HashMap<>();
        for (BeamResource child : children) {
            String type = child.getClass().getName();
            if (!resourceByType.containsKey(type)) {
                resourceByType.put(type, new ArrayList<>());
            }

            resourceByType.get(type).add(child);
        }

        return resourceByType;
    }

    public void diffOnCreate(ResourceChange change) throws Exception {
        Map<String, List<BeamResource>> resourceByType = getResourcesByType(getChildren());
        for (String type : resourceByType.keySet()) {
            List<BeamResource> resources = resourceByType.get(type);
            if (resources.size() > 1) {
                change.create(resources);
            } else if (resources.size() == 1) {
                change.createOne(resources.get(0));
            }
        }
    }

    public void diffOnUpdate(ResourceChange change, BeamResource<B> current) throws Exception {
        Map<String, List<BeamResource>> pendingResourceByType = getResourcesByType(getChildren());
        Map<String, List<BeamResource>> currentResourceByType = getResourcesByType(current.getChildren());
        Set<String> types = new HashSet<>(pendingResourceByType.keySet());
        types.addAll(currentResourceByType.keySet());
        for (String type : types) {
            List<BeamResource> pendingList = pendingResourceByType.containsKey(type) ? pendingResourceByType.get(type) : new ArrayList<>();
            List<BeamResource> currentList = currentResourceByType.containsKey(type) ? currentResourceByType.get(type) : new ArrayList<>();
            if (pendingList.size() > 1 || currentList.size() > 1) {
                change.update(currentList, pendingList);
            } else {
                BeamResource pendingResource = null;
                BeamResource currentResource = null;
                if (pendingList.size() == 1) {
                    pendingResource = pendingList.get(0);
                }

                if (currentList.size() == 1) {
                    currentResource = currentList.get(0);
                }

                change.updateOne(currentResource, pendingResource);
            }
        }
    }

    public void diffOnDelete(ResourceChange change) throws Exception {
        Map<String, List<BeamResource>> resourceByType = getResourcesByType(getChildren());
        for (String type : resourceByType.keySet()) {
            List<BeamResource> resources = resourceByType.get(type);
            if (resources.size() > 1) {
                change.delete(resources);
            } else if (resources.size() == 1) {
                change.deleteOne(resources.get(0));
            }
        }
    }

    public boolean isVerifying() {
        return false;
    }

    public BeamResource<B> findCurrent(B cloud, BeamResourceFilter filter) {
        return null;
    }

    public abstract void create(B cloud);

    public abstract void update(B cloud, BeamResource<B> current, Set<String> changedProperties);

    public boolean isDeletable() {
        return true;
    }

    public abstract void delete(B cloud);

    public abstract String toDisplayString();

    public Map<String, BeamReference> getReferences() {
        return references;
    }

    public void setReferences(Map<String, BeamReference> references) {
        this.references = references;
    }

    public Map<String, Object> getUnResolvedProperties() {
        return unResolvedProperties;
    }

    public void setUnResolvedProperties(Map<String, Object> unResolvedProperties) {
        this.unResolvedProperties = unResolvedProperties;
    }

    public BeamContext getContext() {
        return context;
    }

    public void setContext(BeamContext context) {
        this.context = context;
    }

    public void resolveReference() {
        for (String key : getUnResolvedProperties().keySet()) {
            Object value = getUnResolvedProperties().get(key);
            Object resolvedValue = resolve(value, getContext());
            ASTHandler.populate(this, key, resolvedValue);
        }
    }

    public static Object resolve(Object value, BeamContext context) {
        if (value instanceof Map) {
            return resolveMap((Map) value, context);
        } else if (value instanceof List) {
            return resolveList((List) value, context);
        } else if (value instanceof BeamReference) {
            return ((BeamReference) value).resolve(context);
        } else {
            return value;
        }
    }

    private static Map resolveMap(Map map, BeamContext context) {
        Map result = new HashMap<>();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {
                result.put(key, resolveMap((Map) value, context));
            } else if (value instanceof List) {
                result.put(key, resolveList((List) value, context));
            } else if (value instanceof BeamReference) {
                result.put(key, ((BeamReference) value).resolve(context));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    private static List resolveList(List list, BeamContext context) {
        List result = new ArrayList();
        for (Object value : list) {
            if (value instanceof Map) {
                result.add(resolveMap((Map) value, context));
            } else if (value instanceof List) {
                result.add(resolveList((List) value, context));
            } else if (value instanceof BeamReference) {
                result.add(((BeamReference) value).resolve(context));
            } else {
                result.add(value);
            }
        }

        return result;
    }
}
