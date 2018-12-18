package beam.lang.types;

import beam.lang.BeamLanguageException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContainerBlock extends BeamBlock {

    private List<BeamBlock> blocks;

    public List<BeamBlock> getBlocks() {
        if (blocks == null) {
            blocks = new ArrayList<>();
        }

        return blocks;
    }

    public boolean containsKey(String key) {
        return containsKey(key, null);
    }

    public boolean containsKey(String key, String type) {
        for (BeamBlock block : getBlocks()) {
            if (block instanceof ResourceBlock) {
                ResourceBlock resourceBlock = (ResourceBlock) block;
                if (resourceBlock.getResourceIdentifier().equals(key) && resourceBlock.getResourceType().equals(type)) {
                    return true;
                }
            } else if (block instanceof KeyValueBlock && type == null) {
                KeyValueBlock keyValueBlock = (KeyValueBlock) block;
                if (keyValueBlock.getKey().equals(key)) {
                    return true;
                }
            }
        }

        return false;
    }

    public BeamBlock get(String key) {
        return get(key, null);
    }

    public BeamBlock get(String key, String type) {
        for (BeamBlock block : getBlocks()) {
            if (block instanceof ResourceBlock) {
                ResourceBlock resourceBlock = (ResourceBlock) block;
                if (resourceBlock.getResourceIdentifier().equals(key) && resourceBlock.getResourceType().equals(type)) {
                    return resourceBlock;
                }
            } else if (block instanceof KeyValueBlock && type == null) {
                KeyValueBlock keyValueBlock = (KeyValueBlock) block;
                if (keyValueBlock.getKey().equals(key)) {
                    return keyValueBlock;
                }
            }
        }

        return null;
    }

    public void set(String key, String value) {
        Iterator<BeamBlock> iterator = getBlocks().iterator();
        while (iterator.hasNext()) {
            BeamBlock block = iterator.next();

            if (block instanceof KeyValueBlock) {
                KeyValueBlock keyValueBlock = (KeyValueBlock) block;
                if (keyValueBlock.getKey().equals(key)) {
                    keyValueBlock.setValue(new BeamString(value));
                    return;
                }
            }
        }

        KeyValueBlock keyValueBlock = new KeyValueBlock();
        keyValueBlock.setKey(key);
        keyValueBlock.setValue(new BeamString(value));
        getBlocks().add(keyValueBlock);
    }

    @Override
    public boolean resolve() {
        for (BeamBlock child : getBlocks())  {
            boolean resolved = child.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", child);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (BeamBlock block : getBlocks()) {
            sb.append(block.toString());
        }

        return sb.toString();
    }

}
