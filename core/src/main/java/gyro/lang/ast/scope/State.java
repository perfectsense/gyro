package gyro.lang.ast.scope;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import gyro.core.diff.Change;
import gyro.core.diff.Delete;
import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.diff.Replace;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceNames;
import gyro.lang.Resource;

public class State {

    private final RootScope root;
    private final boolean test;
    private final Map<String, FileScope> states = new HashMap<>();

    private String getStateFile(String file) {
        if (file.endsWith(".gyro.state")) {
            return file;

        } else if (file.endsWith(".gyro")) {
            return file + ".state";

        } else {
            return file + ".gyro.state";
        }
    }

    public State(RootScope pending, boolean test) throws Exception {
        root = new RootScope(getStateFile(pending.getFile()));
        this.test = test;

        load(pending, root);
    }

    public boolean isTest() {
        return test;
    }

    private void load(FileScope pending, FileScope state) throws Exception {
        states.put(getStateFile(pending.getFile()), state);
        pending.getBackend().load(state);
        state.getImports().clear();
        state.getPluginLoaders().clear();
        state.getPluginLoaders().addAll(pending.getPluginLoaders());

        for (FileScope pendingImport : pending.getImports()) {
            Path pendingDir = Paths.get(pending.getFile()).getParent() != null ?
                Paths.get(pending.getFile()).getParent() : Paths.get(".");
            Path pendingImportFile = Paths.get(pendingImport.getFile());

            FileScope stateImport = new FileScope(
                    pending,
                    getStateFile(pendingDir.relativize(pendingImportFile).toString()));

            load(pendingImport, stateImport);
            state.getImports().add(stateImport);
        }
    }

    private void save(FileScope state) throws Exception {
        state.getBackend().save(state);

        for (FileScope i : state.getImports()) {
            save(i);
        }
    }

    public void update(Change change) throws Exception {
        if (change instanceof Replace) {
            return;
        }

        Diffable diffable = change.getDiffable();

        if (!(diffable instanceof Resource)) {
            return;
        }

        Resource resource = (Resource) diffable;

        // Delete goes through every state to remove the resource.
        if (change instanceof Delete) {
            String key = resource.resourceIdentifier();

            // Subresource?
            if (key == null) {
                states.values()
                        .stream()
                        .flatMap(s -> s.values().stream())
                        .filter(Resource.class::isInstance)
                        .map(Resource.class::cast)
                        .filter(r -> r.equals(resource.parentResource()))
                        .forEach(r -> updateSubresource(r, resource, true));

            } else {
                for (FileScope state : states.values()) {
                    state.remove(resource.resourceType() + "::" + key);
                }
            }

        } else {
            String key = resource.resourceIdentifier();
            FileScope state = states.get(getStateFile(resource.scope().getFileScope().getFile()));

            // Subresource?
            if (key == null) {
                Resource parent = resource.parentResource();

                updateSubresource(
                        (Resource) state.get(parent.resourceType() + "::" + parent.resourceIdentifier()),
                        resource,
                        false);

            } else {
                state.put(resource.resourceType() + "::" + key, resource);
            }
        }

        save(root);
    }

    private void updateSubresource(Resource parent, Resource subresource, boolean delete) {
        for (DiffableField field : DiffableType.getInstance(parent.getClass()).getFields()) {
            Object value = field.getValue(parent);

            if (value instanceof List) {
                String subresourceName = null;

                if (subresource.getClass().getAnnotation(ResourceNames.class) != null) {
                    for (ResourceName resourceName : subresource.getClass().getAnnotation(ResourceNames.class).value()) {
                        if (resourceName.parent().equals(parent.getClass().getAnnotation(ResourceName.class).value())) {
                            subresourceName = resourceName.value();
                            break;
                        }
                    }
                } else {
                    subresourceName = subresource.getClass().getAnnotation(ResourceName.class).value();
                }

                if (!subresourceName.equals(field.getBeamName())) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                boolean found = false;

                for (ListIterator<Object> i = list.listIterator(); i.hasNext();) {
                    Object item = i.next();

                    if (subresource.equals(item)) {
                        found = true;

                        if (delete) {
                            i.remove();

                        } else {
                            i.set(subresource);
                        }
                    }
                }

                if (!delete && !found) {
                    list.add(subresource);
                }

            } else if (value instanceof Resource) {
                field.setValue(parent, delete ? null : subresource);
            }
        }
    }

    public void swap(RootScope current, RootScope pending, String type, String x, String y) throws Exception {
        swapResources(current, type, x, y);
        swapResources(pending, type, x, y);
        swapResources(root, type, x, y);
        save(root);
    }

    private void swapResources(RootScope rootScope, String type, String xName, String yName) {
        String xFullName = type + "::" + xName;
        String yFullName = type + "::" + yName;
        FileScope xScope = findScope(xFullName, rootScope);
        FileScope yScope = findScope(yFullName, rootScope);

        if (xScope != null && yScope != null) {
            Resource x = (Resource) xScope.get(xFullName);
            Resource y = (Resource) yScope.get(yFullName);

            x.resourceIdentifier(yName);
            y.resourceIdentifier(xName);
            xScope.put(xFullName, y);
            yScope.put(yFullName, x);
        }
    }

    private FileScope findScope(String name, FileScope scope) {
        Object value = scope.get(name);

        if (value instanceof Resource) {
            return scope;
        }

        for (FileScope i : scope.getImports()) {
            FileScope r = findScope(name, i);

            if (r != null) {
                return r;
            }
        }

        return null;
    }

}
