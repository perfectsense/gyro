package beam.lang.ast.scope;

import beam.lang.Resource;
import beam.lang.plugins.PluginLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessScope extends Scope {

    private final List<PluginLoader> pluginLoaders = new ArrayList<>();
    private final Map<String, Class<?>> types = new HashMap<>();
    private final Map<String, Resource> resources = new HashMap<>();

    public ProcessScope(Scope parent, Map<String, Object> values) {
        super(parent, values);
    }

    public ProcessScope(Scope parent) {
        super(parent);
    }

    public List<PluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

    @Override
    public Map<String, Resource> getResources() {
        return resources;
    }

    public Map<String, Class<?>> getTypes() {
        return types;
    }

}
