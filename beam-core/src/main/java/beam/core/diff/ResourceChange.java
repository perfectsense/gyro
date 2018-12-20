package beam.core.diff;

import beam.core.BeamResource;
import beam.lang.types.BeamBlock;
import beam.lang.types.BeamList;
import beam.lang.types.BeamMap;
import beam.lang.types.BeamReference;
import beam.lang.types.BeamValue;
import beam.lang.types.KeyValueBlock;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResourceChange {

    private final BeamResource currentResource;
    private final BeamResource pendingResource;

    private final ResourceDiff diff;
    private final List<ResourceDiff> diffs = new ArrayList<>();
    private boolean changeable;
    private boolean changed;

    private final Set<String> updatedProperties = new HashSet<>();
    private final StringBuilder updatedPropertiesDisplay = new StringBuilder();
    private final Set<String> replacedProperties = new HashSet<>();
    private final StringBuilder replacedPropertiesDisplay = new StringBuilder();

    private final Lazy<BeamResource> changedResource = new Lazy<BeamResource>() {

        @Override
        public final BeamResource create() throws Exception {
            BeamResource resource = change();
            resource.syncPropertiesToInternal();
            changed = true;

            return resource;
        }
    };

    public ResourceChange(ResourceDiff diff, BeamResource currentResource, BeamResource pendingResource) {
        this.currentResource = currentResource;
        this.pendingResource = pendingResource;
        this.diff = diff;
    }

    public BeamResource executeChange() {
        if (isChangeable()) {
            return changedResource.get();

        } else {
            throw new IllegalStateException("Can't change yet!");
        }
    }

    public void create(Collection<BeamResource> pendingResources) throws Exception {
        diff.create(this, pendingResources);
    }

    public void update(Collection currentResources, Collection pendingResources) throws Exception {
        diff.update(this, currentResources, pendingResources);
    }

    public void delete(Collection<BeamResource> pendingResources) throws Exception {
        diff.delete(this, pendingResources);
    }

    public Set<ResourceChange> dependencies() {
        Set<ResourceChange> dependencies = new HashSet<>();

        BeamResource resource = pendingResource != null ? pendingResource : currentResource;
        for (BeamBlock block : (getType() == ChangeType.DELETE ? resource.dependents() : resource.dependencies())) {
            if (block instanceof BeamResource) {
                BeamResource r = (BeamResource) block;
                ResourceChange c = r.getChange();

                if (c != null) {
                    dependencies.add(c);
                }
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

    public static String processAsScalarValue(String key, BeamValue currentValue, BeamValue pendingValue) {
        StringBuilder sb = new StringBuilder();

        Object current = currentValue != null ? currentValue.getValue() : null;
        Object pending = pendingValue.getValue();

        if (pending.equals(current)) {
            return sb.toString();
        }

        sb.append(key);
        sb.append(": ");
        if (ObjectUtils.isBlank(current)) {
            sb.append(pending);
        } else {
            sb.append(current);
            sb.append(" -> ");
            sb.append(pending);
        }

        return sb.toString();
    }

    public static String processAsMapValue(String key, BeamValue currentValue, BeamValue pendingValue) {
        StringBuilder sb = new StringBuilder();

        BeamMap pendingMapValue = (BeamMap) pendingValue;
        Map<String, String> pendingResolvedMap = new HashMap<>();
        for (KeyValueBlock keyValueBlock : pendingMapValue.getKeyValues()) {
            String stringValue = (String) keyValueBlock.getValue().getValue();

            if (stringValue != null) {
                pendingResolvedMap.put(keyValueBlock.getKey(), stringValue);
            } else if (keyValueBlock.getValue() instanceof BeamReference) {
                String ref = String.format("%s %s",
                    ((BeamReference) keyValueBlock.getValue()).getType(),
                    ((BeamReference) keyValueBlock.getValue()).getName());
                pendingResolvedMap.put(keyValueBlock.getKey(), "ref:" + ref);
            }
        }

        BeamMap currentMapValue = (BeamMap) currentValue;
        Map<String, String> currentResolvedMap = new HashMap<>();
        for (KeyValueBlock keyValueBlock : currentMapValue.getKeyValues()) {
            String stringValue = (String) keyValueBlock.getValue().getValue();

            if (stringValue != null) {
                currentResolvedMap.put(keyValueBlock.getKey(), stringValue);
            }
        }

        String diff = mapSummaryDiff(currentResolvedMap, pendingResolvedMap);
        if (!ObjectUtils.isBlank(diff)) {
            sb.append(key);
            sb.append(": ");
            sb.append(diff);
        }

        return sb.toString();
    }

    public static String processAsListValue(String key, BeamValue currentValue, BeamValue pendingValue) {
        StringBuilder sb = new StringBuilder();

        BeamList pendingListValue = (BeamList) pendingValue;
        List<String> pendingResolvedList = new ArrayList<>();
        for (BeamValue value : pendingListValue.getValues()) {
            String stringValue = (String) value.getValue();

            if (stringValue != null) {
                pendingResolvedList.add(stringValue);
            } else if (value instanceof BeamReference) {
                pendingResolvedList.add(value.toString());
            }
        }

        BeamList currentListValue = (BeamList) currentValue;
        List<String> currentResolvedList = new ArrayList<>();
        for (BeamValue value : currentListValue.getValues()) {
            String stringValue = (String) value.getValue();

            if (stringValue != null) {
                currentResolvedList.add(stringValue);
            }
        }

        List<String> additions = new ArrayList<>(pendingResolvedList);
        additions.removeAll(currentResolvedList);

        List<String> subtractions = new ArrayList<>(currentResolvedList);
        subtractions.removeAll(pendingResolvedList);

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
        StringBuilder diffResult = new StringBuilder();
        if (current == null) {
            current = new HashMap();
        }

        final MapDifference diff = Maps.difference(current, pending);
        for (Object key : diff.entriesOnlyOnRight().keySet()) {
            diffResult.append(String.format("+[%s => %s]", key, pending.get(key)));
        }

        for (Object key : diff.entriesOnlyOnLeft().keySet()) {
            diffResult.append(String.format("-[%s => %s]", key, current.get(key)));
        }

        if (diffResult.lastIndexOf(",") == diffResult.length()) {
            diffResult.setLength(diffResult.length() - 2);
        }

        for (Object key : diff.entriesDiffering().keySet()) {
            diffResult.append(String.format("*%s ", key));
            MapDifference.ValueDifference value = (MapDifference.ValueDifference)diff.entriesDiffering().get(key);
            Object currentValue = value.leftValue();
            Object pendingValue = value.rightValue();

            if (currentValue instanceof Map && pendingValue instanceof Map) {
                diffResult.append("(");
                diffResult.append(mapSummaryDiff((Map) currentValue, (Map) pendingValue));
                diffResult.append(") ");
            } else {
                diffResult.append("(");
                diffResult.append(String.format("[%s => %s]", currentValue, pendingValue));
                diffResult.append(") ");
            }
        }

        return diffResult.toString();
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

    protected BeamResource change() {
        ChangeType type = getType();

        if (type == ChangeType.UPDATE) {
            pendingResource.resolve();
            pendingResource.update(currentResource, updatedProperties);
            pendingResource.syncPropertiesToInternal();
            return pendingResource;

        } else if (type == ChangeType.REPLACE) {
            return currentResource;

        } else {
            return currentResource;
        }
    }

}
