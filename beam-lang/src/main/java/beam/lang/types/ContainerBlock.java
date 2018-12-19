package beam.lang.types;

import beam.lang.BeamLanguageException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ContainerBlock extends BeamBlock {

    private Map<ResourceKey, ResourceBlock> resources = new HashMap<>();
    private Map<String, BeamValue> keyValues = new HashMap<>();

    public boolean containsKey(String key) {
        return keyValues.containsKey(key);
    }

    public boolean containsResource(String key, String type) {
        ResourceKey resourceKey = new ResourceKey(type, key);
        if (resources.containsKey(resourceKey)) {
            return true;
        }

        return false;
    }

    public Set<String> keys() {
        return keyValues.keySet();
    }

    public BeamValue get(String key) {
        return keyValues.get(key);
    }

    public Object getValue(String key) {
        BeamValue value = get(key);
        if (value != null) {
            return value.getValue();
        }

        return null;
    }

    public void putKeyValue(KeyValueBlock keyValueBlock) {
        keyValueBlock.setParentBlock(this);
        keyValues.put(keyValueBlock.getKey(), keyValueBlock.getValue());
    }

    public void put(String key, String value) {
        keyValues.put(key, new BeamString(value));
    }

    public Collection<ResourceBlock> resources() {
        return resources.values();
    }

    public void putResource(ResourceBlock resourceBlock) {
        resourceBlock.setParentBlock(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public ResourceBlock getResource(String key, String type) {
        ResourceKey resourceKey = new ResourceKey(type, key);
        return resources.get(resourceKey);
    }

    @Override
    public boolean resolve() {
        for (ResourceBlock resourceBlock : resources.values()) {
            boolean resolved = resourceBlock.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", resourceBlock);
            }
        }

        for (BeamValue value : keyValues.values()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", value);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (ResourceBlock resourceBlock : resources.values()) {
            sb.append(resourceBlock.toString());
        }

        for (String key : keyValues.keySet()) {
            sb.append(key).append(": ");
            sb.append(get(key).toString());
            sb.append("\n");
        }

        return sb.toString();
    }

}
