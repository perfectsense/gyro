package beam.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ResourceContainer extends Container {

    transient Map<ResourceKey, Resource> resources = new HashMap<>();

    public Collection<Resource> resources() {
        return resources.values();
    }

    public Resource removeResource(Resource block) {
        return resources.remove(block.resourceKey());
    }

    public void putResource(Resource resourceBlock) {
        resourceBlock.parentNode(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public void putResourceKeepParent(Resource resourceNode) {
        resources.put(resourceNode.resourceKey(), resourceNode);
    }

    public Resource resource(String type, String key) {
        ResourceKey resourceKey = new ResourceKey(type, key);

        return resources.get(resourceKey);
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
    public void evaluateControlNodes() {
        super.evaluateControlNodes();

        for (Resource resourceNode : resources()) {
            resourceNode.evaluateControlNodes();
        }
    }

    @Override
    public String serialize(int indent) {
        StringBuilder sb = new StringBuilder();

        for (Resource resourceBlock : resources()) {
            sb.append(resourceBlock.serialize(indent));
        }

        sb.append(super.serialize(indent));

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("ResourceContainer[resources: %d]", resources().size());
    }

}
