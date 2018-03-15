package beam.core;

import java.util.HashSet;
import java.util.Set;

public abstract class BeamResourceFilter extends BeamObject {

    private String type;
    private Set<String> includedLayers;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getIncludedLayers() {
        if (includedLayers == null) {
            includedLayers = new HashSet<>();
        }

        return includedLayers;
    }

    public void setIncludedLayers(Set<String> includedLayers) {
        this.includedLayers = includedLayers;
    }

    public boolean isInclude(Object resource) {
        return true;
    }
}
