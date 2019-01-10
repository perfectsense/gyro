package beam.lang;

import beam.core.BeamResource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ResourceContainerNode extends ContainerNode {

    transient Map<ResourceKey, BeamResource> resources = new HashMap<>();

    public Collection<BeamResource> resources() {
        return resources.values();
    }

    public BeamResource removeResource(BeamResource block) {
        return resources.remove(block.resourceKey());
    }

    public void putResource(BeamResource resourceBlock) {
        resourceBlock.setParentNode(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public void putResourceKeepParent(BeamResource resourceNode) {
        resources.put(resourceNode.resourceKey(), resourceNode);
    }

    public BeamResource getResource(String type, String key) {
        ResourceKey resourceKey = new ResourceKey(type, key);

        return resources.get(resourceKey);
    }

    @Override
    public void evaluateControlNodes() {
        super.evaluateControlNodes();

        for (BeamResource resourceNode : resources()) {
            resourceNode.evaluateControlNodes();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (BeamResource resourceBlock : resources()) {
            sb.append(resourceBlock.toString());
        }

        sb.append(super.toString());

        return sb.toString();
    }

}
