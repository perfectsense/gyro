package beam.core.diff;

import java.util.HashSet;
import java.util.Set;

public class ResourceDisplayDiff {

    private Set<String> changedProperties = new HashSet<>();
    private StringBuilder changedDisplay = new StringBuilder();
    private boolean isReplace;

    public Set<String> getChangedProperties() {
        return changedProperties;
    }

    public void addChangedProperty(String property) {
        changedProperties.add(property);
    }

    public StringBuilder getChangedDisplay() {
        return changedDisplay;
    }

    public String changedDisplay() {
        return getChangedDisplay().toString();
    }

    public boolean isReplace() {
        return isReplace;
    }

    public void setReplace(boolean replace) {
        isReplace = replace;
    }

}
