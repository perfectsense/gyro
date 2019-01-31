package beam.lang.ast.scope;

import beam.core.LocalStateBackend;
import beam.lang.Resource;
import beam.lang.StateBackend;
import beam.lang.plugins.PluginLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileScope extends Scope {

    private final String file;
    private FileScope state;
    private final List<FileScope> imports = new ArrayList<>();
    private StateBackend stateBackend = new LocalStateBackend();
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

    public StateBackend getStateBackend() {
        return stateBackend;
    }

    public void setStateBackend(StateBackend stateBackend) {
        this.stateBackend = stateBackend;
    }

    public List<PluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

}
