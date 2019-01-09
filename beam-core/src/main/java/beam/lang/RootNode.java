package beam.lang;

import beam.core.BeamResource;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RootNode extends ContainerNode {

    private transient String path;
    private transient RootNode state;

    transient Map<ResourceKey, ResourceNode> resources = new HashMap<>();
    transient Map<String, RootNode> imports = new HashMap<>();

    public String path() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public RootNode state() {
        if (state == null) {
            return this;
        }

        return state;
    }

    public void setState(RootNode state) {
        this.state = state;
    }

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

    public ResourceNode getResource(String type, String key) {
        ResourceKey resourceKey = new ResourceKey(type, key);

        ResourceNode resourceNode = resources.get(resourceKey);
        if (resourceNode == null && imports().containsKey("_")) {
            // Check in "global" import
            RootNode importNode = imports().get("_");
            resourceNode = importNode.resources.get(resourceKey);
        }

        return resourceNode;
    }

    public Map<String, RootNode> imports() {
        return imports;
    }

    public void putImport(String key, RootNode rootNode) {
        rootNode.setParentNode(this);

        imports.put(key, rootNode);
    }

    public RootNode getImport(String key) {
        return imports.get(key);
    }

    public String importPath(String currentPath) {
        Path importPath = new File(path).toPath();
        Path otherPath  = new File(currentPath).getParentFile().toPath();

        return otherPath.relativize(importPath).toString().replace(".bcl", "");
    }

    @Override
    public void copyNonResourceState(ContainerNode source) {
        super.copyNonResourceState(source);

        if (source instanceof RootNode) {
            RootNode rootNode = (RootNode) source;

            imports.putAll(rootNode.imports);

            for (ResourceNode resourceNode : rootNode.resources()) {
                if (!(resourceNode instanceof BeamResource)) {
                    putResource(resourceNode);
                }
            }
        }
    }

    @Override
    public ValueNode get(String key) {
        ValueNode value = super.get(key);
        if (value == null && imports().containsKey("_")) {
            // Check in "global" import
            RootNode importNode = imports().get("_");
            value = importNode.get(key);
        }

        return value;
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

        for (String importName : imports.keySet()) {
            RootNode importNode = imports.get(importName);

            String importPath = importNode.importPath(path) + ".bcl.state";

            sb.append("import ");
            sb.append(importPath);

            if (!importName.equals(importPath)) {
                sb.append(" as ");
                sb.append(importName);
            }

            sb.append("\n");
        }

        sb.append("\n");

        for (ResourceNode resourceBlock : resources.values()) {
            sb.append(resourceBlock.toString());
        }

        sb.append(super.toString());

        return sb.toString();
    }

}
