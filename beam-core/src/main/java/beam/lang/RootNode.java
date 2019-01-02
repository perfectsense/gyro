package beam.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RootNode extends ContainerNode {

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

    public ResourceNode getResource(String key, String type) {
        ResourceKey resourceKey = new ResourceKey(type, key);
        return resources.get(resourceKey);
    }

    public void copyNonResourceState(ContainerNode source) {
        keyValues.putAll(source.keyValues);
    }

    @Override
    public boolean resolve() {
        super.resolve();

        for (ResourceNode resourceBlock : resources.values()) {
            boolean resolved = resourceBlock.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", resourceBlock);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (ResourceNode resourceBlock : resources.values()) {
            sb.append(resourceBlock.toString());
        }

        sb.append(super.toString());

        return sb.toString();
    }

}
