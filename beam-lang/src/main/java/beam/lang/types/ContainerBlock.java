package beam.lang.types;

import beam.lang.BeamLanguageException;

import java.util.ArrayList;
import java.util.List;

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
