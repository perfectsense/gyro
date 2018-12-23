package beam.lang.nodes;

import beam.lang.BeamLanguageException;

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

        for (Map.Entry<String, Object> entry : resolvedKeyValues().entrySet()) {
            Object value = entry.getValue();

            if (value != null) {
                sb.append("    ").append(entry.getKey()).append(": ");

                if (value instanceof String) {
                    sb.append("'" + entry.getValue() + "'");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(entry.getValue());
                } else if (value instanceof Map) {
                    sb.append(mapToString((Map) value));
                } else if (value instanceof List) {
                    sb.append(listToString((List) value));
                } else if (value instanceof ResourceNode) {
                    sb.append(((ResourceNode) value).resourceKey());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

}
