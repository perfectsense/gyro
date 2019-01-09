package beam.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ResourceContainerNode extends ContainerNode {

    transient Map<ResourceKey, ResourceNode> resources = new HashMap<>();

    public Collection<ResourceNode> resources() {
        return resources.values();
    }

    public ResourceNode removeResource(ResourceNode block) {
        return resources.remove(block.resourceKey());
    }

    public void putResource(ResourceNode resourceBlock) {
        resourceBlock.setParentNode(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public void putResourceKeepParent(ResourceNode resourceNode) {
        resources.put(resourceNode.resourceKey(), resourceNode);
    }

    public ResourceNode getResource(String type, String key) {
        ResourceKey resourceKey = new ResourceKey(type, key);

        return resources.get(resourceKey);
    }

    @Override
    public void evaluateControlNodes() {
        super.evaluateControlNodes();

        for (ResourceNode resourceNode : resources()) {
            resourceNode.evaluateControlNodes();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (ResourceNode resourceBlock : resources()) {
            sb.append(resourceBlock.toString());
        }

        sb.append(super.toString());

        return sb.toString();
    }

}
