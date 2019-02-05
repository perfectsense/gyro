package beam.core.diff;

import beam.core.BeamCore;
import beam.lang.Resource;
import beam.lang.ast.scope.State;

import java.util.ArrayList;
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
        this.currentResources = currentResources != null
                ? sortResources(currentResources)
                : Collections.emptyList();

        this.pendingResources = pendingResources != null
                ? sortResources(pendingResources)
                : Collections.emptyList();
    }

    private List<Resource> sortResources(Collection<? extends Resource> resources) {
        List<Resource> sorted = new ArrayList<>();

        for (Resource resource : resources) {
            List<Resource> dependencies = new ArrayList<>(resource.dependencies());

            sortResources(dependencies).stream()
                    .filter(r -> !sorted.contains(r))
                    .forEach(sorted::add);

            if (!sorted.contains(resource)) {
                sorted.add(resource);
            }
        }

        return sorted;
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
        Change create = new Change(null, pendingResource) {

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

        for (String key : pendingResource.subresourceFields()) {
            Object pendingValue = pendingResource.get(key);
            Diff diff;

            if (pendingValue instanceof List) {
                diff = new Diff(null, (List<Resource>) pendingValue);

            } else if (pendingValue != null) {
                diff = new Diff(null, Collections.singletonList((Resource) pendingValue));

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                create.getDiffs().add(diff);
            }
        }

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
        Change update = new Change(currentResource, pendingResource);

        update.calculateFieldDiffs();
        currentResource.change(update);
        pendingResource.change(update);

        for (String key : pendingResource.subresourceFields()) {
            Object currentValue = currentResource.get(key);
            Object pendingValue = pendingResource.get(key);
            Diff diff;

            if (pendingValue instanceof Collection) {
                diff = new Diff((List<Resource>) currentValue, (List<Resource>) pendingValue);

            } else if (currentValue != null) {
                if (pendingValue != null) {
                    diff = new Diff(Collections.singletonList((Resource) currentValue), Collections.singletonList((Resource) pendingValue));

                } else {
                    diff = new Diff(Collections.singletonList((Resource) currentValue), null);
                }

            } else if (pendingValue != null) {
                diff = new Diff(null, Collections.singletonList((Resource) pendingValue));

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                update.getDiffs().add(diff);
            }
        }

        return update;
    }

    /**
     * Called when the given {@code asset} needs to be deleted.
     *
     * @param currentResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public Change newDelete(final Resource currentResource) throws Exception {
        Change delete = new Change(currentResource, null) {

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

        for (String key : currentResource.subresourceFields()) {
            Object pendingValue = currentResource.get(key);
            Diff diff;

            if (pendingValue instanceof Collection) {
                diff = new Diff((List<Resource>) pendingValue, null);

            } else if (pendingValue != null) {
                diff = new Diff(Collections.singletonList((Resource) pendingValue), null);

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                delete.getDiffs().add(diff);
            }
        }

        return delete;
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void diff() throws Exception {
        Map<String, Resource> currentResources = getCurrentResources().stream().collect(
                LinkedHashMap::new,
                (map, r) -> map.put(r.primaryKey(), r),
                Map::putAll);

        boolean refreshed = false;

        for (Resource pr : getPendingResources()) {
            Resource cr = currentResources.remove(pr.primaryKey());

            if (cr != null && shouldRefresh()) {
                BeamCore.ui().write(
                        "@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        cr.resourceType(),
                        cr.resourceIdentifier());

                if (!cr.refresh()) {
                    cr = null;
                }

                BeamCore.ui().write("\n");

                refreshed = true;
            }

            changes.add(cr != null
                    ? newUpdate(cr, pr)
                    : newCreate(pr));
        }

        if (refreshed) {
            BeamCore.ui().write("\n");
        }

        for (Resource resource : currentResources.values()) {
            changes.add(newDelete(resource));
        }
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
