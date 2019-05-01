package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;

import gyro.core.plugin.PluginLoader;

public class FileScope extends Scope {

    private final String file;
    private final List<PluginLoader> pluginLoaders = new ArrayList<>();

    public FileScope(RootScope parent, String file) {
        super(parent);
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    public List<PluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

}
