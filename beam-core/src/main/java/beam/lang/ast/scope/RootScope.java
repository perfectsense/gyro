package beam.lang.ast.scope;

import beam.lang.Resource;
import com.psddev.dari.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RootScope extends FileScope {

    private final RootScope current;
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();

    public RootScope(String file) {
        super(null, file);
        this.current = null;
    }

    public RootScope(RootScope current) {
        super(null, StringUtils.removeEnd(current.getFile(), ".state"));
        this.current = current;
    }

    public RootScope getCurrent() {
        return current;
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
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
