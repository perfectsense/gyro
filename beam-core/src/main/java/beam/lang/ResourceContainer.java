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
        resourceBlock.setParentNode(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public void putResourceKeepParent(Resource resourceNode) {
        resources.put(resourceNode.resourceKey(), resourceNode);
    }

    public Resource getResource(String type, String key) {
        ResourceKey resourceKey = new ResourceKey(type, key);

        return resources.get(resourceKey);
    }

    @Override
    public void evaluateControlNodes() {
        super.evaluateControlNodes();

        for (Resource resourceNode : resources()) {
            resourceNode.evaluateControlNodes();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Resource resourceBlock : resources()) {
            sb.append(resourceBlock.toString());
        }

        sb.append(super.toString());

        return sb.toString();
    }

}
