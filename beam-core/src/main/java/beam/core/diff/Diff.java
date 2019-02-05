package beam.core.diff;

import beam.core.BeamCore;
import beam.lang.Resource;
import beam.lang.ast.scope.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class Diff {

    private List<Resource> currentResources;
    private List<Resource> pendingResources;
    private final List<Change> changes = new ArrayList<>();

    private boolean refresh;

    public Diff(List<Resource> currentResources, List<Resource> pendingResources) {
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
        return currentResources;
    }

    /**
     * Returns all resources that should be applied from configs.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public List<Resource> getPendingResources() {
        return pendingResources;
    }

    /**
     * Called when a new asset needs to be created based on the given
     * {@code config}.
     *
     * @param pendingResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public Change newCreate(final Resource pendingResource) throws Exception {
        Change create = new Change(this, null, pendingResource) {

            @Override
            protected Resource change() {
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
    public Change newUpdate(final Resource currentResource, final Resource pendingResource) throws Exception {
        pendingResource.syncPropertiesFromResource(currentResource);

        Change update = new Change(this, currentResource, pendingResource);
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
    public Change newDelete(final Resource currentResource) throws Exception {
        Change delete = new Change(this, currentResource, null) {

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

    public void create(Change change, List<Resource> pendingResources) throws Exception {
        Diff diff = new Diff(null, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void createOne(Change change, Resource pendingResource) throws Exception {
        if (pendingResource != null) {
            Diff diff = new Diff(null, Arrays.asList(pendingResource));
            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public void update(Change change, List currentResources, List pendingResources) throws Exception {
        Diff diff = new Diff(currentResources, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void updateOne(Change change, Resource currentResource, Resource pendingResource) throws Exception {
        if (currentResource != null) {
            if (pendingResource != null) {
                Diff diff = new Diff(Arrays.asList(currentResource), Arrays.asList(pendingResource));
                diff.diff();
                change.getDiffs().add(diff);
            } else {
                deleteOne(change, currentResource);
            }

        } else if (pendingResource != null) {
            createOne(change, pendingResource);
        }
    }

    public void delete(Change change, List<Resource> currentResources) throws Exception {
        Diff diff = new Diff(currentResources, null);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void deleteOne(Change change, Resource currentResource) throws Exception {
        if (currentResource != null) {
            Diff diff = new Diff(Arrays.asList(currentResource), null);
            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void diff() throws Exception {
        sortPendingResources();
        sortCurrentResources();
        diffResources();
    }

    public boolean hasChanges() {
        List<Change> changes = getChanges();

        for (Change change : changes) {
            if (change.getType() != ChangeType.KEEP) {
                return true;
            }
        }

        for (Change change : changes) {
            for (Diff diff : change.getDiffs()) {
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

                Change change = currentResource != null ? newUpdate(currentResource, pendingResource) : newCreate(pendingResource);

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
                Change change = newDelete(resource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }
    }

    private void writeChange(Change change) {
        switch (change.getType()) {
            case CREATE :
                BeamCore.ui().write("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    BeamCore.ui().write(" * %s", change);
                } else {
                    BeamCore.ui().write("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                BeamCore.ui().write("@|blue * %s|@", change);
                break;

            case DELETE :
                BeamCore.ui().write("@|red - %s|@", change);
                break;

            default :
                BeamCore.ui().write(change.toString());
        }
    }

    public Set<ChangeType> write() {
        Set<ChangeType> changeTypes = new HashSet<>();

        if (!hasChanges()) {
            return changeTypes;
        }

        for (Change change : getChanges()) {
            ChangeType type = change.getType();
            List<Diff> changeDiffs = change.getDiffs();

            if (type == ChangeType.KEEP) {
                boolean hasChanges = false;

                for (Diff changeDiff : changeDiffs) {
                    if (changeDiff.hasChanges()) {
                        hasChanges = true;
                        break;
                    }
                }

                if (!hasChanges) {
                    continue;
                }
            }

            changeTypes.add(type);
            writeChange(change);

            BeamCore.ui().write("\n");
            BeamCore.ui().indented(() -> changeDiffs.forEach(d -> changeTypes.addAll(d.write())));
        }

        return changeTypes;
    }

    private void setChangeable() {
        for (Change change : getChanges()) {
            change.setChangeable(true);
            change.getDiffs().forEach(Diff::setChangeable);
        }
    }

    public void executeCreateOrUpdate(State state) throws Exception {
        setChangeable();

        for (Change change : getChanges()) {
            ChangeType type = change.getType();

            if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                execute(state, change);
            }

            for (Diff d : change.getDiffs()) {
                d.executeCreateOrUpdate(state);
            }
        }
    }

    public void executeDelete(State state) throws Exception {
        setChangeable();

        for (ListIterator<Change> j = getChanges().listIterator(getChanges().size()); j.hasPrevious();) {
            Change change = j.previous();

            for (Diff d : change.getDiffs()) {
                d.executeDelete(state);
            }

            if (change.getType() == ChangeType.DELETE) {
                execute(state, change);
            }
        }
    }

    private void execute(State state, Change change) throws Exception {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<Change> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (Change d : dependencies) {
                execute(state, d);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        change.executeChange();
        BeamCore.ui().write(" OK\n");
        state.update(change);
    }

}
