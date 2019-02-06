package beam.lang.ast.scope;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import beam.core.diff.Change;
import beam.core.diff.Delete;
import beam.core.diff.Diffable;
import beam.core.diff.DiffableField;
import beam.core.diff.DiffableType;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

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
            String key = resource.resourceIdentifier();
            Map<String, Resource> stateResources = states.get(resource.scope().getFileScope().getFile()).getResources();

            // Subresource?
            if (key == null) {
                updateSubresource(
                        stateResources.get(resource.parentResource().resourceIdentifier()),
                        resource,
                        false);

            } else {
                stateResources.put(key, resource);
            }
        }

        save(root);
    }

    private void updateSubresource(Resource parent, Resource subresource, boolean delete) {
        for (DiffableField field : DiffableType.getInstance(parent.getClass()).getFields()) {
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
