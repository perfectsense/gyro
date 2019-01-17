package beam.lang;

import beam.core.LocalStateBackend;
import beam.lang.plugins.PluginLoader;
import beam.lang.types.Value;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BeamFile extends ResourceContainer {

    private transient String path;
    private transient BeamFile state;
    private StateBackend stateBackend;
    private Set<PluginLoader> plugins;

    transient Map<String, BeamFile> imports = new HashMap<>();

    public String path() {
        return path;
    }

    public void path(String path) {
        this.path = path;
    }

    public BeamFile state() {
        if (state == null) {
            return this;
        }

        return state;
    }

    public void state(BeamFile state) {
        this.state = state;
    }

    public StateBackend stateBackend() {
        if (stateBackend == null) {
            return new LocalStateBackend();
        }

        return stateBackend;
    }

    public void stateBackend(StateBackend stateBackend) {
        this.stateBackend = stateBackend;
    }

    public Set<PluginLoader> plugins() {
        if (plugins == null) {
            plugins = new HashSet<>();
        }

        return plugins;
    }

    public Map<String, BeamFile> imports() {
        return imports;
    }

    public void putImport(String key, BeamFile fileNode) {
        fileNode.parent(this);

        imports.put(key, fileNode);
    }

    public BeamFile importFile(String key) {
        return imports.get(key);
    }

    public String importPath(String currentPath) {
        Path importPath = new File(path).toPath();
        Path otherPath  = new File(currentPath).getParentFile().toPath();

        return otherPath.relativize(importPath).toString().replace(".bcl", "");
    }

    @Override
    public Resource resource(String type, String key) {
        Resource resource = super.resource(type, key);
        if (resource == null && imports().containsKey("_")) {
            // Check in "global" import
            BeamFile importNode = imports().get("_");
            resource = importNode.resource(type, key);
        }

        return resource;
    }

    @Override
    public void copyNonResourceState(Container source) {
        super.copyNonResourceState(source);

        if (source instanceof BeamFile) {
            BeamFile fileNode = (BeamFile) source;

            imports().putAll(fileNode.imports());
            plugins().addAll(fileNode.plugins());
            stateBackend(fileNode.stateBackend());
        }
    }

    @Override
    public Value get(String key) {
        Value value = super.get(key);
        if (value == null && imports().containsKey("_")) {
            // Check in "global" import
            BeamFile importNode = imports().get("_");
            value = importNode.get(key);
        }

        return value;
    }

    @Override
    public boolean resolve() {
        super.resolve();

        for (Resource resource : resources()) {
            boolean resolved = resource.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", resource);
            }
        }

        return true;
    }

    @Override
    public String serialize(int indent) {
        StringBuilder sb = new StringBuilder();

        for (String importName : imports.keySet()) {
            BeamFile importNode = imports.get(importName);

            String importPath = importNode.importPath(path) + ".bcl.state";

            sb.append("import ");
            sb.append(importPath);

            if (!importName.equals(importPath)) {
                sb.append(" as ");
                sb.append(importName);
            }

            sb.append("\n");
        }

        for (PluginLoader pluginLoader : plugins()) {
            sb.append(pluginLoader);
        }

        sb.append("\n");
        sb.append(super.serialize(indent));

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("BeamFile[%s]", path);
    }

}
