package beam.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ControlStructure extends ResourceContainer {

    private Map<String, List<Resource>> subResources;

    public abstract void evaluate();

    public Map<String, List<Resource>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public void putSubResource(Resource subresource) {
        String type = subresource.resourceType();
        List<Resource> subresources = subResources().computeIfAbsent(type, s -> new ArrayList<>());
        subresources.add(subresource);
    }

}
