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
    private final boolean test;
    private final Map<String, FileScope> states = new HashMap<>();

    private String getStateFile(String file) {
        if (file.endsWith(".bcl.state")) {
            return file;

        } else if (file.endsWith(".bcl")) {
            return file + ".state";

        } else {
            return file + ".bcl.state";
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
            Path pendingDir = Paths.get(pending.getFile()).getParent();
            Path pendingImportFile = Paths.get(pendingImport.getFile());

            FileScope stateImport = new FileScope(
                    pending,
                    getStateFile(pendingDir.relativize(pendingImportFile).toString()));

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
