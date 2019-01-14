package beam.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceContainer extends Container {

    transient Map<ResourceKey, Resource> resources = new HashMap<>();

    public Collection<Resource> resources() {
        Map<ResourceKey, Resource> allResources = new HashMap<>(resources);

        for (Frame frame : frames()) {
            for (Resource resource : frame.resources()) {
                allResources.put(resource.resourceKey(), resource);
            }
        }

        return allResources.values();
    }

    public Resource removeResource(Resource resource) {
        return resources.remove(resource.resourceKey());
    }

    public void putResource(Resource resource) {
        resource.parent(this);

        resources.put(resource.resourceKey(), resource);
    }

    public void putResourceKeepParent(Resource resource) {
        resources.put(resource.resourceKey(), resource);
    }

    public Resource resource(String type, String key) {
        ResourceKey resourceKey = new ResourceKey(type, key);

        return resources.get(resourceKey);
    }

    @Override
    public boolean resolve() {
        super.resolve();

        for (Resource resource : resources()) {
            if (!resource.resolve()) {
                throw new BeamLanguageException("Unable to resolve configuration.", resource);
            }
        }

        return true;
    }

    @Override
    public void copyNonResourceState(Container source) {
        super.copyNonResourceState(source);

        if (source instanceof ResourceContainer) {
            ResourceContainer resourceContainer = (ResourceContainer) source;

            for (Resource resource : resourceContainer.resources()) {
                if (resource instanceof Credentials) {
                    putResource(resource);
                }
            }
        }
    }

    @Override
    public String serialize(int indent) {
        StringBuilder sb = new StringBuilder();

        for (Resource resource: resources()) {
            sb.append(resource.serialize(indent));
        }

        sb.append(super.serialize(indent));

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("ResourceContainer[resources: %d]", resources().size());
    }

}
