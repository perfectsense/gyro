package beam.core.diff;

import beam.core.BeamCore;
import beam.core.BeamCredentials;
import beam.core.BeamResource;
import beam.lang.BeamFile;
import com.psddev.dari.util.CompactMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ResourceDiff {

    private List<BeamResource> currentResources;
    private List<BeamResource> pendingResources;
    private final List<ResourceChange> changes = new ArrayList<>();

    private BeamFile current;
    private BeamFile pending;
    private boolean refresh;

    public ResourceDiff(BeamFile current, BeamFile pending) {
        this.current = current;
        this.pending = pending;
    }

    public ResourceDiff(List<BeamResource> currentResources,
                        List<BeamResource> pendingResources) {
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
    public List<BeamResource> getCurrentResources() {
        if (currentResources == null && current != null) {
            return findResources(current, true);
        }

        return currentResources;
    }

    /**
     * Returns all resources that should be applied from configs.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public List<BeamResource> getPendingResources() {
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
    public ResourceChange newCreate(final BeamResource pendingResource) throws Exception {
        ResourceChange create = new ResourceChange(this, null, pendingResource) {

            @Override
            protected BeamResource change() {
                pendingResource.resolve();
                pendingResource.create();
                return pendingResource;
            }

            @Override
            public String toString() {
                return String.format("Create %s", pendingResource.toDisplayString());
            }
        };

        pendingResource.setChange(create);
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
    public ResourceChange newUpdate(final BeamResource currentResource, final BeamResource pendingResource) throws Exception {
        pendingResource.syncPropertiesFromResource(currentResource);

        ResourceChange update = new ResourceChange(this, currentResource, pendingResource);
        update.calculateFieldDiffs();

        currentResource.setChange(update);
        pendingResource.setChange(update);
        pendingResource.diffOnUpdate(update, currentResource);

        return update;
    }

    /**
     * Called when the given {@code asset} needs to be deleted.
     *
     * @param currentResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newDelete(final BeamResource currentResource) throws Exception {
        ResourceChange delete = new ResourceChange(this, currentResource, null) {

            @Override
            protected BeamResource change() {
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

        currentResource.setChange(delete);
        currentResource.diffOnDelete(delete);

        return delete;
    }

    public void create(ResourceChange change, List<BeamResource> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(null, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void createOne(ResourceChange change, BeamResource pendingResource) throws Exception {
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

    public void updateOne(ResourceChange change, BeamResource currentResource, BeamResource pendingResource) throws Exception {
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

    public void delete(ResourceChange change, List<BeamResource> currentResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(currentResources, null);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void deleteOne(ResourceChange change, BeamResource currentResource) throws Exception {
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
        sortResources();
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

    private List<BeamResource> sortResources(Collection<? extends BeamResource> resources) {
        List<BeamResource> sorted = new ArrayList<>();

        for (BeamResource resource : resources) {
            List<BeamResource> deps = new ArrayList<>();
            for (BeamResource dependency : resource.dependencies()) {
                deps.add(dependency);
            }

            for (BeamResource r : sortResources(deps)) {
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

    private void sortResources() {
        List<BeamResource> pending = getPendingResources();
        if (pending != null) {
            pendingResources = sortResources(pending);
        }
    }

    private void diffResources() throws Exception {
        Map<String, BeamResource> currentResourcesByName = new CompactMap<>();
        Iterable<? extends BeamResource> currentResources = getCurrentResources();

        if (currentResources != null) {
            for (BeamResource resource : currentResources) {
                currentResourcesByName.put(resource.primaryKey(), resource);
            }
        }

        Iterable<? extends BeamResource> pendingResources = getPendingResources();

        if (pendingResources != null) {
            for (BeamResource pendingResource : pendingResources) {
                BeamResource currentResource = currentResourcesByName.remove(pendingResource.primaryKey());

                if (currentResource != null && shouldRefresh()) {
                    BeamCore.ui().write("@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        currentResource.resourceType(), currentResource.resourceIdentifier());

                    if (!currentResource.refresh()) {
                        currentResource = null;
                    }

                    BeamCore.ui().write("\n");
                }

                pendingResource.syncPropertiesFromResource(currentResource);
                pendingResource.resolve();

                ResourceChange change = currentResource != null ? newUpdate(currentResource, pendingResource) : newCreate(pendingResource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }

        if (currentResources != null) {
            for (BeamResource resource : currentResourcesByName.values()) {
                ResourceChange change = newDelete(resource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }
    }

    private List<BeamResource> findResources(BeamFile fileNode, boolean loadState) {
        List<BeamResource> resources = new ArrayList<>();

        for (BeamResource resource : fileNode.resources()) {
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

    private void removeBeamCredentials(List<BeamResource> resources) {
        Iterator<BeamResource> iter = resources.iterator();
        while (iter.hasNext()) {
            BeamResource resource = iter.next();

            if (resource instanceof BeamCredentials) {
                iter.remove();
            }
        }
    }

}
