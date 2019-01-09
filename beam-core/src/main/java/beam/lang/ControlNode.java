package beam.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ControlNode extends ResourceContainerNode {

    private Map<String, List<ResourceNode>> subResources;

    public abstract void evaluate();

    public Map<String, List<ResourceNode>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public void putSubResource(ResourceNode subresource) {
        String type = subresource.resourceType();
        List<ResourceNode> subresources = subResources().computeIfAbsent(type, s -> new ArrayList<>());
        subresources.add(subresource);
    }

}
