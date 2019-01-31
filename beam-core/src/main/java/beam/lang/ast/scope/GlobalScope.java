package beam.lang.ast.scope;

import beam.lang.Resource;
import beam.lang.plugins.PluginLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalScope extends Scope {

    private final List<PluginLoader> pluginLoaders = new ArrayList<>();
    private final Map<String, Class<?>> typeClasses = new HashMap<>();
    private final Map<String, Resource> resources = new HashMap<>();

    public GlobalScope() {
        super(null);
    }

    public List<PluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

    public Map<String, Class<?>> getTypeClasses() {
        return typeClasses;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

}
