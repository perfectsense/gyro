package beam.core.diff;

import beam.core.BeamCore;
import beam.lang.BeamFile;
import beam.lang.Credentials;
import beam.lang.Modification;
import beam.lang.Resource;
import beam.lang.ast.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceDiff {

    private List<Resource> currentResources;
    private List<Resource> pendingResources;
    private final List<ResourceChange> changes = new ArrayList<>();

    private BeamFile current;
    private BeamFile pending;
    private boolean refresh;

    public ResourceDiff(BeamFile current, BeamFile pending) {
        this.current = current;
        this.pending = pending;
    }

    public ResourceDiff(Scope current, Scope pending) {
        this.currentResources = new ArrayList<>(current.getResources().values());
        this.pendingResources = new ArrayList<>(pending.getResources().values());
    }

    public ResourceDiff(List<Resource> currentResources,
                        List<Resource> pendingResources) {
        this.currentResources = currentResources;
        this.pendingResources = pendingResources != null ? pendingResources : Collections.EMPTY_LIST;
    }

    public boolean shouldRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    /**
     * Returns all resources that are currently running in providers.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public List<Resource> getCurrentResources() {
        if (currentResources == null && current != null) {
            return findResources(current, true);
        }

        removeBeamCredentials(currentResources);

        return currentResources;
    }

    /**
     * Returns all resources that should be applied from configs.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public List<Resource> getPendingResources() {
        if (pendingResources == null && pending != null) {
            return findResources(pending, false);
        }

        removeBeamCredentials(pendingResources);

        return pendingResources;
    }

    /**
     * Called when a new asset needs to be created based on the given
     * {@code config}.
     *
     * @param pendingResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newCreate(final Resource pendingResource) throws Exception {
        ResourceChange create = new ResourceChange(this, null, pendingResource) {

            @Override
            protected Resource change() {
                pendingResource.resolve();
                pendingResource.create();
                return pendingResource;
            }

            @Override
            public String toString() {
                return String.format("Create %s", pendingResource.toDisplayString());
            }
        };

        pendingResource.change(create);
        pendingResource.diffOnCreate(create);

        return create;
    }

    /**
     * Called when the given {@code asset} needs to be updated based on the
     * given {@code config}.
     *
     * @param currentResource Can't be {@code null}.
     * @param pendingResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newUpdate(final Resource currentResource, final Resource pendingResource) throws Exception {
        pendingResource.syncPropertiesFromResource(currentResource);

        ResourceChange update = new ResourceChange(this, currentResource, pendingResource);
        update.calculateFieldDiffs();

        currentResource.change(update);
        pendingResource.change(update);
        pendingResource.diffOnUpdate(update, currentResource);

        return update;
    }

    /**
     * Called when the given {@code asset} needs to be deleted.
     *
     * @param currentResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newDelete(final Resource currentResource) throws Exception {
        ResourceChange delete = new ResourceChange(this, currentResource, null) {

            @Override
            protected Resource change() {
                currentResource.delete();
                return currentResource;
            }

            @Override
            public String toString() {
                return String.format(
                    "Delete %s",
                    currentResource.toDisplayString());
            }
        };

        currentResource.change(delete);
        currentResource.diffOnDelete(delete);

        return delete;
    }

    public void create(ResourceChange change, List<Resource> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(null, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void createOne(ResourceChange change, Resource pendingResource) throws Exception {
        if (pendingResource != null) {
            ResourceDiff diff = new ResourceDiff(null, Arrays.asList(pendingResource));
            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public void update(ResourceChange change, List currentResources, List pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(currentResources, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void updateOne(ResourceChange change, Resource currentResource, Resource pendingResource) throws Exception {
        if (currentResource != null) {
            if (pendingResource != null) {
                ResourceDiff diff = new ResourceDiff(Arrays.asList(currentResource), Arrays.asList(pendingResource));
                diff.diff();
                change.getDiffs().add(diff);
            } else {
                deleteOne(change, currentResource);
            }

        } else if (pendingResource != null) {
            createOne(change, pendingResource);
        }
    }

    public void delete(ResourceChange change, List<Resource> currentResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(currentResources, null);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void deleteOne(ResourceChange change, Resource currentResource) throws Exception {
        if (currentResource != null) {
            ResourceDiff diff = new ResourceDiff(Arrays.asList(currentResource), null);
            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public List<ResourceChange> getChanges() {
        return changes;
    }

    public void diff() throws Exception {
        sortPendingResources();
        sortCurrentResources();
        diffResources();
    }

    public boolean hasChanges() {
        List<ResourceChange> changes = getChanges();

        for (ResourceChange change : changes) {
            if (change.getType() != ChangeType.KEEP) {
                return true;
            }
        }

        for (ResourceChange change : changes) {
            for (ResourceDiff diff : change.getDiffs()) {
                if (diff.hasChanges()) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<Resource> sortResources(Collection<? extends Resource> resources) {
        List<Resource> sorted = new ArrayList<>();

        for (Resource resource : resources) {
            List<Resource> deps = new ArrayList<>();
            for (Resource dependency : resource.dependencies()) {
                deps.add(dependency);
            }

            for (Resource r : sortResources(deps)) {
                if (!sorted.contains(r)) {
                    sorted.add(r);
                }
            }

            if (!sorted.contains(resource)) {
                sorted.add(resource);
            }
        }

        return sorted;
    }

    private void sortPendingResources() {
        List<Resource> pending = getPendingResources();
        if (pending != null) {
            pendingResources = sortResources(pending);
        }
    }

    private void sortCurrentResources() {
        List<Resource> current = getCurrentResources();
        if (current != null) {
            currentResources = sortResources(current);
        }
    }

    private void diffResources() throws Exception {
        Map<String, Resource> currentResourcesByName = new LinkedHashMap<>();
        Iterable<? extends Resource> currentResources = getCurrentResources();

        if (currentResources != null) {
            for (Resource resource : currentResources) {
                currentResourcesByName.put(resource.primaryKey(), resource);
            }
        }

        Iterable<? extends Resource> pendingResources = getPendingResources();

        boolean refreshed = false;
        if (pendingResources != null) {
            for (Resource pendingResource : pendingResources) {
                Resource currentResource = currentResourcesByName.remove(pendingResource.primaryKey());

                if (currentResource != null && shouldRefresh()) {
                    BeamCore.ui().write("@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        currentResource.resourceType(), currentResource.resourceIdentifier());

                    if (!currentResource.refresh()) {
                        currentResource = null;
                    }

                    BeamCore.ui().write("\n");

                    refreshed = true;
                }

                pendingResource.syncPropertiesFromResource(currentResource);
                pendingResource.resolve();

                ResourceChange change = currentResource != null ? newUpdate(currentResource, pendingResource) : newCreate(pendingResource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }

        if (refreshed) {
            BeamCore.ui().write("\n");
        }

        if (currentResources != null) {
            for (Resource resource : currentResourcesByName.values()) {
                ResourceChange change = newDelete(resource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }
    }

    private List<Resource> findResources(BeamFile fileNode, boolean loadState) {
        List<Resource> resources = new ArrayList<>();

        for (Resource resource : fileNode.resources()) {
            resources.add(resource);
        }

        for (BeamFile importedNode : fileNode.imports().values()) {
            if (loadState && importedNode.state() != null) {
                resources.addAll(findResources(importedNode.state(), loadState));
            } else {
                resources.addAll(findResources(importedNode, loadState));
            }
        }

        removeBeamCredentials(resources);

        return resources;
    }

    private void removeBeamCredentials(List<Resource> resources) {
        if (resources == null) {
            return;
        }

        Iterator<Resource> iter = resources.iterator();
        while (iter.hasNext()) {
            Resource resource = iter.next();

            if (resource instanceof Credentials || resource instanceof Modification) {
                iter.remove();
            }
        }
    }

}
