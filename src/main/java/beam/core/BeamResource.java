package beam.core;

import beam.parser.ASTHandler;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import beam.core.diff.ResourceChange;

public abstract class BeamResource<B extends BeamProvider> extends BeamObject implements BeamReferable {

    private BeamResource<B> parent;
    private List<BeamResource<B>> children = new ArrayList<>();
    private final Set<BeamResource<B>> dependencies = new HashSet<>();
    private final Set<BeamResource<B>> dependents = new HashSet<>();
    private String beamId;
    private ResourceChange change;
    private Map<String, BeamReference> references = new HashMap<>();
    private Map<String, BeamReferable> unResolvedProperties = new ConcurrentHashMap<>();
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

    public Map<String, BeamReferable> getUnResolvedProperties() {
        return unResolvedProperties;
    }

    public void setUnResolvedProperties(Map<String, BeamReferable> unResolvedProperties) {
        this.unResolvedProperties = unResolvedProperties;
    }

    public BeamContext getContext() {
        return context;
    }

    public void setContext(BeamContext context) {
        this.context = context;
    }

    public boolean resolve(BeamContext context) {
        boolean progress = false;
        for (String key : getUnResolvedProperties().keySet()) {
            BeamReferable referable = getUnResolvedProperties().get(key);
            progress = progress || referable.resolve(context);
            if (referable.getValue() != null) {
                ASTHandler.populate(this, key, referable.getValue());
                getUnResolvedProperties().remove(key);
                progress = true;
            }
        }

        return progress;
    }

    @Override
    public Object getValue() {
        return this;
    }
}
