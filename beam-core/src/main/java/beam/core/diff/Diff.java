package beam.core.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import beam.core.BeamCore;
import beam.lang.Resource;
import beam.lang.ast.scope.State;
import com.psddev.dari.util.ObjectUtils;

public class Diff {

    private final List<Resource> currentResources;
    private final List<Resource> pendingResources;
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

    public Diff(Resource currentResource, Resource pendingResource) {
        this.currentResources = currentResource != null
                ? Collections.singletonList(currentResource)
                : Collections.emptyList();

        this.pendingResources = pendingResource != null
                ? Collections.singletonList(pendingResource)
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

    public List<Change> getChanges() {
        return changes;
    }

    public void diff() throws Exception {
        Map<String, Resource> currentResources = this.currentResources.stream().collect(
                LinkedHashMap::new,
                (map, r) -> map.put(r.primaryKey(), r),
                Map::putAll);

        boolean refreshed = false;

        for (Resource pr : pendingResources) {
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

    @SuppressWarnings("unchecked")
    private Change newCreate(Resource resource) throws Exception {
        Create create = new Create(resource);

        resource.change(create);

        for (DiffableField field : DiffableType.getInstance(resource.getClass()).getFields()) {
            if (!field.isSubresource()) {
                continue;
            }

            Object value = field.getValue(resource);
            Diff diff;

            if (value instanceof List) {
                diff = new Diff(null, (List<Resource>) value);

            } else if (value != null) {
                diff = new Diff(null, (Resource) value);

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

    @SuppressWarnings("unchecked")
    private Change newUpdate(Resource currentResource, Resource pendingResource) throws Exception {
        ResourceDisplayDiff displayDiff = diffFields(currentResource, pendingResource);
        Set<String> changedProperties = displayDiff.getChangedProperties();
        String changedDisplay = displayDiff.getChangedDisplay().toString();
        Change change;

        if (changedProperties.isEmpty()) {
            change = new Keep(pendingResource);

        } else if (displayDiff.isReplace()) {
            change = new Replace(currentResource, pendingResource, changedProperties, changedDisplay);

        } else {
            change = new Update(currentResource, pendingResource, changedProperties, changedDisplay);
        }

        currentResource.change(change);
        pendingResource.change(change);

        for (DiffableField field : DiffableType.getInstance(currentResource.getClass()).getFields()) {
            if (!field.isSubresource()) {
                continue;
            }

            Object currentValue = field.getValue(currentResource);
            Object pendingValue = field.getValue(pendingResource);
            Diff diff;

            if (pendingValue instanceof List) {
                diff = new Diff((List<Resource>) currentValue, (List<Resource>) pendingValue);

            } else if (currentValue != null) {
                if (pendingValue != null) {
                    diff = new Diff((Resource) currentValue, (Resource) pendingValue);

                } else {
                    diff = new Diff((Resource) currentValue, null);
                }

            } else if (pendingValue != null) {
                diff = new Diff(null, (Resource) pendingValue);

            } else {
                diff = null;
            }

            if (diff != null) {
                diff.diff();
                change.getDiffs().add(diff);
            }
        }

        return change;
    }

    private ResourceDisplayDiff diffFields(Resource currentResource, Resource pendingResource) {
        boolean firstField = true;
        ResourceDisplayDiff displayDiff = new ResourceDisplayDiff();

        for (DiffableField field : DiffableType.getInstance(currentResource.getClass()).getFields()) {
            if (field.isSubresource()) {
                continue;
            }

            Object currentValue = field.getValue(currentResource);
            Object pendingValue = field.getValue(pendingResource);

            if (pendingValue != null || field.isNullable()) {
                String key = field.getBeamName();
                String fieldChangeOutput;

                if (pendingValue instanceof List) {
                    fieldChangeOutput = Change.processAsListValue(key, (List) currentValue, (List) pendingValue);

                } else if (pendingValue instanceof Map) {
                    fieldChangeOutput = Change.processAsMapValue(key, (Map) currentValue, (Map) pendingValue);

                } else {
                    fieldChangeOutput = Change.processAsScalarValue(key, currentValue, pendingValue);
                }

                if (!ObjectUtils.isBlank(fieldChangeOutput)) {
                    if (!firstField) {
                        displayDiff.getChangedDisplay().append(", ");
                    }

                    displayDiff.addChangedProperty(key);
                    displayDiff.getChangedDisplay().append(fieldChangeOutput);

                    if (!field.isUpdatable()) {
                        displayDiff.setReplace(true);
                    }

                    firstField = false;
                }
            }
        }

        return displayDiff;
    }

    @SuppressWarnings("unchecked")
    private Change newDelete(Resource resource) throws Exception {
        Delete delete = new Delete(resource);

        resource.change(delete);

        for (DiffableField field : DiffableType.getInstance(resource.getClass()).getFields()) {
            if (!field.isSubresource()) {
                continue;
            }

            Object value = field.getValue(resource);
            Diff diff;

            if (value instanceof List) {
                diff = new Diff((List<Resource>) value, null);

            } else if (value != null) {
                diff = new Diff((Resource) value, null);

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

    public boolean hasChanges() {
        List<Change> changes = getChanges();

        for (Change change : changes) {
            if (!(change instanceof Keep)) {
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

    public Set<ChangeType> write() {
        Set<ChangeType> changeTypes = new HashSet<>();

        if (!hasChanges()) {
            return changeTypes;
        }

        for (Change change : getChanges()) {
            List<Diff> changeDiffs = change.getDiffs();

            if (change instanceof Keep) {
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

            if (change instanceof Create) {
                changeTypes.add(ChangeType.CREATE);

            } else if (change instanceof Delete) {
                changeTypes.add(ChangeType.DELETE);

            } else if (change instanceof Keep) {
                changeTypes.add(ChangeType.KEEP);

            } else if (change instanceof Replace) {
                changeTypes.add(ChangeType.REPLACE);

            } else if (change instanceof Update) {
                changeTypes.add(ChangeType.UPDATE);
            }

            change.writeTo(BeamCore.ui());
            BeamCore.ui().write("\n");
            BeamCore.ui().indented(() -> changeDiffs.forEach(d -> changeTypes.addAll(d.write())));
        }

        return changeTypes;
    }

    private void setChangeable() {
        for (Change change : getChanges()) {
            change.setExecutable(true);
            change.getDiffs().forEach(Diff::setChangeable);
        }
    }

    public void executeCreateOrUpdate(State state) throws Exception {
        setChangeable();

        for (Change change : getChanges()) {
            if (change instanceof Create || change instanceof Update) {
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

            if (change instanceof Delete) {
                execute(state, change);
            }
        }
    }

    private void execute(State state, Change change) throws Exception {
        if (change instanceof Keep || change instanceof Replace || change.isChanged()) {
            return;
        }

        Set<Change> dependencies = change.getDependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (Change d : dependencies) {
                execute(state, d);
            }
        }

        BeamCore.ui().write("Executing: ");
        change.writeTo(BeamCore.ui());
        change.execute();
        BeamCore.ui().write(" OK\n");
        state.update(change);
    }

}
