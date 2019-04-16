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
    private final Map<String, Resource> resources = new LinkedHashMap<>();

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

    public Map<String, Resource> getResources() {
        return resources;
    }

    public List<Resource> findAllResources() {
        return new ArrayList<>(resources.values());
    }

    public Resource findResource(String name) {
        return resources.get(name);
    }

}
