package beam.lang;

import beam.core.BeamLocalState;
import beam.core.BeamProvider;
import beam.core.BeamResource;
import beam.core.BeamState;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileNode extends ResourceContainerNode {

    private transient String path;
    private transient FileNode state;
    private BeamState stateBackend;
    private List<BeamProvider> providers;

    transient Map<String, FileNode> imports = new HashMap<>();

    public String path() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public FileNode state() {
        if (state == null) {
            return this;
        }

        return state;
    }

    public void setState(FileNode state) {
        this.state = state;
    }

    public BeamState stateBackend() {
        if (stateBackend == null) {
            return new BeamLocalState();
        }

        return stateBackend;
    }

    public void stateBackend(BeamState stateBackend) {
        this.stateBackend = stateBackend;
    }

    public List<BeamProvider> providers() {
        if (providers == null) {
            providers = new ArrayList<>();
        }

        return providers;
    }

    public Map<String, FileNode> imports() {
        return imports;
    }

    public void putImport(String key, FileNode fileNode) {
        fileNode.setParentNode(this);

        imports.put(key, fileNode);
    }

    public FileNode getImport(String key) {
        return imports.get(key);
    }

    public String importPath(String currentPath) {
        Path importPath = new File(path).toPath();
        Path otherPath  = new File(currentPath).getParentFile().toPath();

        return otherPath.relativize(importPath).toString().replace(".bcl", "");
    }

    @Override
    public BeamResource getResource(String type, String key) {
        BeamResource resource = super.getResource(type, key);
        if (resource == null && imports().containsKey("_")) {
            // Check in "global" import
            FileNode importNode = imports().get("_");
            resource = importNode.getResource(type, key);
        }

        return resource;
    }

    @Override
    public void copyNonResourceState(ContainerNode source) {
        super.copyNonResourceState(source);

        if (source instanceof FileNode) {
            FileNode fileNode = (FileNode) source;

            imports().putAll(fileNode.imports());
            providers().addAll(fileNode.providers());
            stateBackend(fileNode.stateBackend());
        }
    }

    @Override
    public ValueNode get(String key) {
        ValueNode value = super.get(key);
        if (value == null && imports().containsKey("_")) {
            // Check in "global" import
            FileNode importNode = imports().get("_");
            value = importNode.get(key);
        }

        return value;
    }

    @Override
    public boolean resolve() {
        super.resolve();

        for (BeamResource resource : resources()) {
            boolean resolved = resource.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", resource);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (String importName : imports.keySet()) {
            FileNode importNode = imports.get(importName);

            String importPath = importNode.importPath(path) + ".bcl.state";

            sb.append("import ");
            sb.append(importPath);

            if (!importName.equals(importPath)) {
                sb.append(" as ");
                sb.append(importName);
            }

            sb.append("\n");
        }

        for (BeamProvider provider : providers()) {
            sb.append(provider);
        }

        sb.append("\n");
        sb.append(super.toString());

        return sb.toString();
    }

}
