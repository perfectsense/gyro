package beam.lang.ast.scope;

import beam.core.diff.ChangeType;
import beam.core.diff.Change;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import beam.lang.ResourceField;
import beam.lang.ResourceType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class State {

    private final RootScope root;
    private final Map<String, FileScope> states = new HashMap<>();

    public State(FileScope pending) throws Exception {
        root = new RootScope(pending.getFile() + ".state");

        load(pending, root);
    }

    private void load(FileScope pending, FileScope state) throws Exception {
        states.put(pending.getFile(), state);
        pending.getBackend().load(state);
        state.clear();
        state.getImports().clear();
        state.getPluginLoaders().clear();
        state.getPluginLoaders().addAll(pending.getPluginLoaders());

        for (FileScope pendingImport : pending.getImports()) {
            Path pendingDir = Paths.get(pending.getFile()).getParent();
            Path pendingImportFile = Paths.get(pendingImport.getFile());

            FileScope stateImport = new FileScope(
                    pending,
                    pendingDir.relativize(pendingImportFile).toString() + ".state");

            load(pendingImport, stateImport);
            state.getImports().add(stateImport);
        }
    }

    public void update(Change change) throws Exception {

        // Delete goes through every state to remove the resource.
        if (change.getType() == ChangeType.DELETE) {
            Resource resource = change.getCurrentResource();
            String key = resource.resourceIdentifier();

            // Subresource?
            if (key == null) {
                for (FileScope state : states.values()) {
                    for (Resource parent : state.getResources().values()) {
                        updateSubresource(parent, resource, true);
                    }
                }

            } else {
                for (FileScope state : states.values()) {
                    state.getResources().remove(key);
                }
            }

        } else {
            Resource resource = change.getPendingResource();
            String key = resource.resourceIdentifier();
            Map<String, Resource> stateResources = states.get(resource.scope().getFileScope().getFile()).getResources();

            // Subresource?
            if (key == null) {
                updateSubresource(
                        stateResources.get(resource.parent().resourceIdentifier()),
                        resource,
                        false);

            } else {
                stateResources.put(key, resource);
            }
        }

        save(root);
    }

    private void updateSubresource(Resource parent, Resource subresource, boolean delete) {
        for (ResourceField field : ResourceType.getInstance(parent.getClass()).getFields()) {
            Object value = field.getValue(parent);

            if (value instanceof List) {
                String subresourceName = subresource.getClass().getAnnotation(ResourceName.class).value();

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

    private void save(FileScope state) throws Exception {
        state.getBackend().save(state);

        for (FileScope i : state.getImports()) {
            save(i);
        }
    }

}
