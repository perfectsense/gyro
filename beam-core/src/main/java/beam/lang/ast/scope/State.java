package beam.lang.ast.scope;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
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

        try {
            pending.getBackend().load(state);

        } catch (FileNotFoundException | NoSuchFileException error) {
            // No state file yet because first run.
        }

        state.clear();
        state.getImports().clear();
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

    public void update(ResourceChange change) throws Exception{
        boolean delete = change.getType() == ChangeType.DELETE;
        Resource resource = delete ? change.getCurrentResource() : change.getPendingResource();
        String key = resource.resourceIdentifier();

        if (key != null) {
            if (delete) {
                states.values().forEach(s -> s.getResources().remove(key));

            } else {
                states.get(resource.scope().getFileScope().getFile())
                        .getResources()
                        .put(key, resource);
            }
        }

        save(root);
    }

    private void save(FileScope state) throws Exception {
        state.getBackend().save(state);

        for (FileScope i : state.getImports()) {
            save(i);
        }
    }

}
