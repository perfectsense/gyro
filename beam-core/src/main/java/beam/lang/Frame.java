package beam.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Frame extends ResourceContainer {

    private Map<String, List<Resource>> subResources;

    public Map<String, List<Resource>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public void putSubresource(Resource subresource) {
        subresource.parent(this);
        String type = subresource.resourceType();
        List<Resource> subresources = subResources().computeIfAbsent(type, s -> new ArrayList<>());
        subresources.add(subresource);
    }

    @Override
    public boolean resolve() {
        super.resolve();

        for (List<Resource> resources : subResources().values()) {
            for (Resource resource : resources) {
                if (!resource.resolve()) {
                    throw new BeamLanguageException("Unable to resolve configuration.", resource);
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("Frame[key/values: %d, resources: %d, subresources: %d]", keys().size(), resources().size(), subResources().size());
    }

}
