package gyro.core.scope;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.FileBackend;
import gyro.core.diff.Change;
import gyro.core.diff.Delete;
import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.diff.Replace;
import gyro.core.plugin.PluginLoader;
import gyro.core.resource.ResourceName;
import gyro.core.resource.ResourceNames;
import gyro.core.resource.Resource;

public class State {

    private final FileBackend backend;
    private final RootScope root;
    private final boolean test;
    private final Map<String, FileScope> states = new HashMap<>();
    private final Set<String> diffFiles;

    public State(RootScope current, RootScope pending, boolean test, Set<String> diffFiles) throws Exception {
        this.backend = current.getBackend();
        this.root = new RootScope(current.getFile(), backend, null, current.getLoadFiles());
        this.test = test;
        this.diffFiles = diffFiles != null ? ImmutableSet.copyOf(diffFiles) : null;

        root.load();

        for (FileScope state : root.getFileScopes()) {
            states.put(state.getFile(), state);
        }

        for (FileScope state : pending.getFileScopes()) {
            String stateFile = state.getFile();

            if (!states.containsKey(stateFile)) {
                states.put(stateFile, new FileScope(root, stateFile));
            }
        }
    }

    public boolean isTest() {
        return test;
    }

    public Set<String> getDiffFiles() {
        return diffFiles;
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
            FileScope state = states.get(resource.scope().getFileScope().getFile());

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

        save();
    }

    private void save() throws IOException {
        for (FileScope state : states.values()) {
            String file = state.getFile();

            try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                    backend.openOutput(file),
                    StandardCharsets.UTF_8))) {

                for (PluginLoader pluginLoader : state.getPluginLoaders()) {
                    out.write(pluginLoader.toString());
                }

                for (Object value : state.values()) {
                    if (value instanceof Resource) {
                        out.write(((Resource) value).toNode().toString());
                    }
                }
            }
        }
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

            if (value instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;

                if (delete) {
                    collection.removeIf(subresource::equals);

                } else if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    boolean found = false;

                    for (ListIterator<Object> i = list.listIterator(); i.hasNext();) {
                        Object item = i.next();

                        if (subresource.equals(item)) {
                            i.set(subresource);
                            found = true;
                        }
                    }

                    if (!found) {
                        list.add(subresource);
                    }

                } else {
                    collection.removeIf(subresource::equals);
                    collection.add(subresource);
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
        save();
    }

    private void swapResources(RootScope rootScope, String type, String xName, String yName) {
        String xFullName = type + "::" + xName;
        String yFullName = type + "::" + yName;
        FileScope xScope = findFileScope(rootScope, xFullName);
        FileScope yScope = findFileScope(rootScope, yFullName);

        if (xScope != null && yScope != null) {
            Resource x = (Resource) xScope.get(xFullName);
            Resource y = (Resource) yScope.get(yFullName);

            x.resourceIdentifier(yName);
            y.resourceIdentifier(xName);
            xScope.put(xFullName, y);
            yScope.put(yFullName, x);
        }
    }

    private FileScope findFileScope(RootScope rootScope, String name) {
        for (FileScope fileScope : rootScope.getFileScopes()) {
            Object value = fileScope.get(name);

            if (value instanceof Resource) {
                return fileScope;
            }
        }

        return null;
    }

}
