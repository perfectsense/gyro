package gyro.core.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceFinder;
import gyro.core.workflow.Workflow;
import gyro.lang.ast.block.VirtualResourceNode;

public class RootScope extends Scope {

    private final RootScope current;
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Class<? extends ResourceFinder>> resourceFinderClasses = new HashMap<>();
    private final Map<String, VirtualResourceNode> virtualResourceNodes = new LinkedHashMap<>();
    private final List<Workflow> workflows = new ArrayList<>();
    private final List<FileScope> fileScopes = new ArrayList<>();
    private final FileScope initScope;

    public RootScope() {
        super(null);
        this.current = null;

        try {
            initScope = new FileScope(this, GyroCore.findPluginPath().toString());
        } catch (IOException e) {
            throw new GyroException("Unable to create init scope!");
        }

        put("ENV", System.getenv());
    }

    public RootScope(RootScope current) {
        super(null);
        this.current = current;
        try {
            initScope = new FileScope(this, GyroCore.findPluginPath().toString());
        } catch (IOException e) {
            throw new GyroException("Unable to create init scope!");
        }

        put("ENV", System.getenv());
    }

    public RootScope getCurrent() {
        return current;
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public Map<String, Class<? extends ResourceFinder>> getResourceFinderClasses() {
        return resourceFinderClasses;
    }

    public Map<String, VirtualResourceNode> getVirtualResourceNodes() {
        return virtualResourceNodes;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public List<FileScope> getFileScopes() {
        return fileScopes;
    }

    public FileScope getInitScope() {
        return initScope;
    }

    public List<Resource> findAllResources() {
        List<Resource> resources = new ArrayList<>();
        for (FileScope scope : getFileScopes()) {
            addResources(resources, scope);
        }
        return resources;
    }

    private void addResources(List<Resource> resources, FileScope scope) {
        scope.getImports().forEach(i -> addResources(resources, i));

        scope.values()
                .stream()
                .filter(Resource.class::isInstance)
                .map(Resource.class::cast)
                .forEach(resources::add);
    }

    public Resource findResource(String name) {
        Resource resource;
        for (FileScope scope : getFileScopes()) {
            resource = findResourceInScope(name, scope);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private Resource findResourceInScope(String name, FileScope scope) {
        Object value = scope.get(name);

        if (value instanceof Resource) {
            return (Resource) value;
        }

        for (FileScope s : scope.getImports()) {
            Resource resource = findResourceInScope(name, s);

            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

}
