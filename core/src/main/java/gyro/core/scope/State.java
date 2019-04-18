package gyro.core.scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import gyro.core.FileBackend;
import gyro.core.GyroCore;
import gyro.core.LocalFileBackend;
import gyro.core.diff.Change;
import gyro.core.diff.Delete;
import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.diff.Replace;
import gyro.core.resource.ResourceName;
import gyro.core.resource.ResourceNames;
import gyro.core.resource.Resource;

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

    private Path getStatePath(Path file) throws IOException {
        Path rootDir = GyroCore.findPluginPath().getParent().getParent();
        Path relative = rootDir.relativize(file.toAbsolutePath());
        Path statePath = Paths.get(rootDir.toString(), ".gyro", "state", relative.toString());
        Files.createDirectories(statePath.getParent());

        return statePath;
    }

    public State(RootScope pending, boolean test) throws Exception {
        root = new RootScope(new HashSet<>(pending.getActiveScopePaths()));
        this.test = test;

        load(pending, root);
    }

    public boolean isTest() {
        return test;
    }

    private void load(RootScope pending, RootScope state) throws Exception {
        pending.getInitScope().getBackend().load(state);
        for (FileScope fileScope : state.getFileScopes()) {
            states.put(fileScope.getFile(), fileScope);
        }

        for (FileScope fileScope : pending.getFileScopes()) {
            String statePath = getStatePath(Paths.get(getStateFile(fileScope.getFile()))).toString();
            if (!states.containsKey(statePath)) {
                FileScope scope = new FileScope(state, statePath);
                states.put(statePath, scope);
                state.getFileScopes().add(scope);
            }
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
            FileScope state = states.get(getStatePath(Paths.get(getStateFile(resource.scope().getFileScope().getFile()))).toString());

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

        root.getInitScope().getBackend().save(root);
    }

    private void updateSubresource(Resource parent, Resource subresource, boolean delete) {
        for (DiffableField field : DiffableType.getInstance(parent.getClass()).getFields()) {
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

            if (!field.getGyroName().equals(subresourceName)) {
                continue;
            }

            Object value = field.getValue(parent);

            if (value instanceof List) {
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
        root.getInitScope().getBackend().save(root);
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

    private FileScope findScope(String name, RootScope scope) {
        for (FileScope fileScope : scope.getFileScopes()) {
            Object value = fileScope.get(name);

            if (value instanceof Resource) {
                return fileScope;
            }
        }

        return null;
    }

}
