package beam.core.diff;

import beam.lang.Resource;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResourceChange {

    private final Resource currentResource;
    private final Resource pendingResource;

    private final ResourceDiff diff;
    private final List<ResourceDiff> diffs = new ArrayList<>();
    private boolean changeable;
    private boolean changed;

    private final Set<String> updatedProperties = new HashSet<>();
    private final StringBuilder updatedPropertiesDisplay = new StringBuilder();
    private final Set<String> replacedProperties = new HashSet<>();
    private final StringBuilder replacedPropertiesDisplay = new StringBuilder();

    private final Lazy<Resource> changedResource = new Lazy<Resource>() {

        @Override
        public final Resource create() throws Exception {
            Resource resource = change();
            changed = true;

            return resource;
        }
    };

    public ResourceChange(ResourceDiff diff, Resource currentResource, Resource pendingResource) {
        this.currentResource = currentResource;
        this.pendingResource = pendingResource;
        this.diff = diff;
    }

    public Resource executeChange() {
        if (isChangeable()) {
            return changedResource.get();

        } else {
            throw new IllegalStateException("Can't change yet!");
        }
    }

    public void create(List<Resource> pendingResources) throws Exception {
        diff.create(this, pendingResources);
    }

    public void createOne(Resource pendingResource) throws Exception {
        diff.createOne(this, pendingResource);
    }

    public void update(List currentResources, List pendingResources) throws Exception {
        diff.update(this, currentResources, pendingResources);
    }

    public void updateOne(Resource currentResource, Resource pendingResource) throws Exception {
        diff.updateOne(this, currentResource, pendingResource);
    }

    public void delete(List<Resource> pendingResources) throws Exception {
        diff.delete(this, pendingResources);
    }

    public void deleteOne(Resource pendingResource) throws Exception {
        diff.deleteOne(this, pendingResource);
    }

    public Set<ResourceChange> dependencies() {
        Set<ResourceChange> dependencies = new HashSet<>();

        Resource resource = pendingResource != null ? pendingResource : currentResource;
        for (Resource r : (getType() == ChangeType.DELETE ? resource.dependents() : resource.dependencies())) {
            ResourceChange c = r.change();

            if (c != null) {
                dependencies.add(c);
            }
        }

        return dependencies;
    }

    public ChangeType getType() {
        if (pendingResource == null) {
            return ChangeType.DELETE;
        } else if (currentResource == null) {
            return ChangeType.CREATE;
        } else if (updatedPropertiesDisplay.length() > 0) {
            return ChangeType.UPDATE;
        } else if (replacedPropertiesDisplay.length() > 0) {
            return ChangeType.REPLACE;
        } else {
            return ChangeType.KEEP;
        }
    }

    public List<ResourceDiff> getDiffs() {
        return diffs;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public void setChangeable(boolean changeable) {
        this.changeable = changeable;
    }

    public boolean isChanged() {
        return changed;
    }

    /**
     * Calculate the difference between individual fields.
     *
     * If a field changed and that field is updatable, it's added to updatedProperties.
     * If a field changed and that field is not updatable, it's added to replaceProperties.
     */
    public void calculateFieldDiffs() {
        if (currentResource == null || pendingResource == null) {
            return;
        }

        ResourceDisplayDiff displayDiff = pendingResource.calculateFieldDiffs(currentResource);

        if (displayDiff.isReplace()) {
            replacedProperties.addAll(displayDiff.getChangedProperties());
            replacedPropertiesDisplay.append(displayDiff.getChangedDisplay());
        } else {
            updatedProperties.addAll(displayDiff.getChangedProperties());
            updatedPropertiesDisplay.append(displayDiff.getChangedDisplay());
        }
    }

    public static String processAsScalarValue(String key, Object currentValue, Object pendingValue) {
        StringBuilder sb = new StringBuilder();

        if (pendingValue.equals(currentValue)) {
            return sb.toString();
        }

        sb.append(key);
        sb.append(": ");
        if (ObjectUtils.isBlank(currentValue)) {
            sb.append(pendingValue);
        } else {
            sb.append(currentValue);
            sb.append(" -> ");
            sb.append(pendingValue);
        }

        return sb.toString();
    }

    public static String processAsMapValue(String key, Map currentValue, Map pendingValue) {
        StringBuilder sb = new StringBuilder();

        String diff = mapSummaryDiff(currentValue, pendingValue);
        if (!ObjectUtils.isBlank(diff)) {
            sb.append(key);
            sb.append(": ");
            sb.append(diff);
        }

        return sb.toString();
    }

    public static String processAsListValue(String key, List currentValue, List pendingValue) {
        StringBuilder sb = new StringBuilder();

        List<String> additions = new ArrayList<>(pendingValue);
        additions.removeAll(currentValue);

        List<String> subtractions = new ArrayList<>(currentValue);
        subtractions.removeAll(pendingValue);

        if (!additions.isEmpty()) {
            sb.append(key);
            sb.append(": ");
            sb.append("+[");
            sb.append(StringUtils.join(additions, ", "));
            sb.append("]");
        } else if (!subtractions.isEmpty()) {
            sb.append(key);
            sb.append(": ");
            sb.append("-[");
            sb.append(StringUtils.join(subtractions, ", "));
            sb.append("]");
        }

        return sb.toString();
    }

    public static String mapSummaryDiff(Map current, Map pending) {
        if (current == null) {
            current = new HashMap();
        }

        final MapDifference diff = Maps.difference(current, pending);

        List<String> diffs = new ArrayList<>();

        for (Object key : diff.entriesOnlyOnRight().keySet()) {
            diffs.add(String.format("+[%s => %s]", key, pending.get(key)));
        }

        for (Object key : diff.entriesOnlyOnLeft().keySet()) {
            diffs.add(String.format("-[%s => %s]", key, current.get(key)));
        }

        for (Object key : diff.entriesDiffering().keySet()) {
            StringBuilder diffResult = new StringBuilder();
            diffResult.append(String.format("*[%s: ", key));
            MapDifference.ValueDifference value = (MapDifference.ValueDifference)diff.entriesDiffering().get(key);
            Object currentValue = value.leftValue();
            Object pendingValue = value.rightValue();

            if (currentValue instanceof Map && pendingValue instanceof Map) {
                diffResult.append(mapSummaryDiff((Map) currentValue, (Map) pendingValue));
            } else {
                diffResult.append(String.format("%s => %s", currentValue, pendingValue));
            }

            diffResult.append("]");

            diffs.add(diffResult.toString());
        }

        return StringUtils.join(diffs, ", ");
    }

    @Override
    public String toString() {
        ChangeType type = getType();

        if (type == ChangeType.UPDATE) {
            return String.format(
                "Update %s (%s)",
                currentResource.toDisplayString(),
                updatedPropertiesDisplay);

        } else if (type == ChangeType.REPLACE) {
            return String.format(
                "Replace %s (%s)",
                currentResource.toDisplayString(),
                replacedPropertiesDisplay);

        } else {
            return currentResource.toDisplayString();
        }
    }

    protected Resource change() {
        ChangeType type = getType();

        if (type == ChangeType.UPDATE) {
            pendingResource.update(currentResource, updatedProperties);
            return pendingResource;

        } else if (type == ChangeType.REPLACE) {
            return currentResource;

        } else {
            return currentResource;
        }
    }

}
