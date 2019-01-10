package beam.lang;

import beam.core.BeamResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ControlNode extends ResourceContainerNode {

    private Map<String, List<BeamResource>> subResources;

    public abstract void evaluate();

    public Map<String, List<BeamResource>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public void putSubResource(BeamResource subresource) {
        String type = subresource.resourceType();
        List<BeamResource> subresources = subResources().computeIfAbsent(type, s -> new ArrayList<>());
        subresources.add(subresource);
    }

}
