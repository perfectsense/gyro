package beam.lang.ast.scope;

import beam.core.LocalFileBackend;
import beam.lang.Resource;
import beam.lang.FileBackend;
import beam.lang.plugins.PluginLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileScope extends Scope {

    private final String file;
    private FileScope state;
    private final List<FileScope> imports = new ArrayList<>();
    private FileBackend fileBackend = new LocalFileBackend();
    private final List<PluginLoader> pluginLoaders = new ArrayList<>();
    private final Map<String, Resource> resources = new HashMap<>();

    public FileScope(Scope parent, String file) {
        super(parent);
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    public FileScope getState() {
        if (state == null) {
            state = new FileScope(getFileScope(), getFile());
        }
        return state;
    }

    public void setState(FileScope state) {
        this.state = state;
    }

    public List<FileScope> getImports() {
        return imports;
    }

    public FileBackend getFileBackend() {
        return fileBackend;
    }

    public void setFileBackend(FileBackend fileBackend) {
        this.fileBackend = fileBackend;
    }

    public List<PluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

}
