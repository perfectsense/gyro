package gyro.lang.ast.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gyro.lang.Resource;
import gyro.lang.ResourceFinder;
import gyro.lang.Workflow;
import gyro.lang.ast.block.VirtualResourceNode;
import com.psddev.dari.util.StringUtils;

public class RootScope extends FileScope {

    private final RootScope current;
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Class<? extends ResourceFinder>> resourceFinderClasses = new HashMap<>();
    private final Map<String, VirtualResourceNode> virtualResourceNodes = new LinkedHashMap<>();
    private final List<Workflow> workflows = new ArrayList<>();

    public RootScope(String file) {
        super(null, file);
        this.current = null;

        put("ENV", System.getenv());
    }

    public RootScope(RootScope current) {
        super(null, StringUtils.removeEnd(current.getFile(), ".state"));
        this.current = current;

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

    public List<Resource> findAllResources() {
        List<Resource> resources = new ArrayList<>();
        addResources(resources, this);
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
        return findResourceInScope(name, this);
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
