package beam.lang.types;

import beam.lang.BeamLanguageException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerBlock extends BeamBlock {

    Map<ResourceKey, ResourceBlock> resources = new HashMap<>();
    Map<String, BeamValue> keyValues = new HashMap<>();

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

    public void putList(String key, List<String> listValue) {
        BeamList list = new BeamList();
        list.setParentBlock(this);

        for (String item : listValue) {
            if (item.startsWith("$")) {
                list.getValues().add(new BeamReference(item));
            } else {
                list.getValues().add(new BeamString(item));
            }
        }

        KeyValueBlock block = new KeyValueBlock();
        block.setKey(key);
        block.setValue(list);

        putKeyValue(block);
    }

    public void putMap(String key, Map<String, Object> mapValue) {
        BeamMap map = new BeamMap();
        map.setParentBlock(this);

        for (String mapKey : mapValue.keySet()) {
            KeyValueBlock item = new KeyValueBlock();

            item.setKey(mapKey);
            item.setValue(new BeamString(mapValue.get(mapKey).toString()));

            map.getKeyValues().add(item);
        }

        KeyValueBlock block = new KeyValueBlock();
        block.setKey(key);
        block.setValue(map);
        putKeyValue(block);
    }

    public Collection<ResourceBlock> resources() {
        return resources.values();
    }

    public ResourceBlock removeResource(ResourceBlock block) {
        return resources.remove(block.resourceKey());
    }

    public void putResource(ResourceBlock resourceBlock) {
        resourceBlock.setParentBlock(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public ResourceBlock getResource(String key, String type) {
        ResourceKey resourceKey = new ResourceKey(type, key);
        return resources.get(resourceKey);
    }

    public void copyNonResourceState(ContainerBlock source) {
        keyValues.putAll(source.keyValues);
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
