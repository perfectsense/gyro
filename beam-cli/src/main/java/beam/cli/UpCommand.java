package beam.cli;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.BeamState;
import beam.core.BeamLocalState;
import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.lang.BCL;
import beam.lang.BeamConfig;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamResolvable;
import beam.lang.BeamReferable;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    @Option(name = { "--refresh" })
    public boolean refresh;

    private final Set<ChangeType> changeTypes = new HashSet<>();

    private BeamState stateBackend;

    private Map<String, BeamContext> pendingStates;

    @Override
    public void doExecute() throws Exception {

        if (getArguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        BeamCore core = new BeamCore();
        core.processConfig(getArguments().get(0));
        Set<BeamResource> resources = new TreeSet<>();
        Set<BeamResource> current = new TreeSet<>();
        Map<String, BeamConfig> configMap = new HashMap<>();
        configMap.putAll(core.getConfigs());
        pendingStates = new HashMap<>();

        for (String fileName : configMap.keySet()) {
            String stateName = fileName + ".state";
            BeamConfig root = configMap.get(fileName);

            BeamConfig pendingState = new BeamConfig();
            for (BeamContextKey key : root.listContextKeys()) {
                BeamReferable referable = root.getReferable(key);
                Object value = referable.getValue();

                if (value instanceof BeamResource) {
                    BeamResource resource = (BeamResource) value;
                    resource.setPath(stateName);
                    resource.setRoot(root);
                    resources.add(resource);
                } else if (value instanceof BeamState) {
                    stateBackend = (BeamState) value;
                } else {
                    pendingState.addReferable(key, referable);
                }
            }

            if (stateBackend == null) {
                stateBackend = new BeamLocalState();
            }

            pendingStates.put(stateName, pendingState);
        }

        stateBackend.load(getArguments().get(0) + ".state", core);
        Map<String, BeamConfig> stateMap = new HashMap<>();
        stateMap.putAll(core.getConfigs());

        for (String fileName : stateMap.keySet()) {
            BeamConfig state = stateMap.get(fileName);
            for (BeamContextKey key : state.listContextKeys()) {
                BeamResolvable resolvable = state.getReferable(key);
                Object value = resolvable.getValue();

                if (value instanceof BeamResource) {
                    BeamResource resource = (BeamResource) value;
                    resource.setPath(fileName);
                    resource.setRoot(state);
                    current.add(resource);
                    if (refresh) {
                        resource.refresh();
                    }
                }
            }
        }

        ResourceDiff diff = new ResourceDiff(current, resources);
        diff.diff();

        changeTypes.clear();

        List<ResourceDiff> diffs = new ArrayList<>();
        diffs.add(diff);

        writeDiffs(diffs);

        boolean hasChanges = false;
        if (changeTypes.contains(ChangeType.CREATE) || changeTypes.contains(ChangeType.UPDATE)) {
            hasChanges = true;

            if (Beam.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to create and/or update resources?")) {
                Beam.ui().write("\n");
                setChangeable(diffs);
                createOrUpdate(diffs, stateMap);
            }
        }

        if (changeTypes.contains(ChangeType.DELETE)) {
            hasChanges = true;

            if (Beam.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to delete resources?")) {
                Beam.ui().write("\n");
                setChangeable(diffs);
                delete(diffs, stateMap);
            }
        }

        if (!hasChanges) {
            Beam.ui().write("\nNo changes.\n");
        }

    }

    public List<String> getArguments() {
        return arguments;
    }

    private void writeDiffs(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            if (!diff.hasChanges()) {
                continue;
            }

            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();
                List<ResourceDiff> changeDiffs = change.getDiffs();

                if (type == ChangeType.KEEP) {
                    boolean hasChanges = false;

                    for (ResourceDiff changeDiff : changeDiffs) {
                        if (changeDiff.hasChanges()) {
                            hasChanges = true;
                            break;
                        }
                    }

                    if (!hasChanges) {
                        continue;
                    }
                }

                changeTypes.add(type);
                writeChange(change);
                Beam.ui().write("\n");
                Beam.ui().indented(() -> writeDiffs(changeDiffs));
            }
        }
    }

    private void writeChange(ResourceChange change) {
        switch (change.getType()) {
            case CREATE :
                Beam.ui().write("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    Beam.ui().write(" * %s", change);
                } else {
                    Beam.ui().write("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                Beam.ui().write("@|blue * %s|@", change);
                break;

            case DELETE :
                Beam.ui().write("@|red - %s|@", change);
                break;

            default :
                Beam.ui().write(change.toString());
        }
    }

    private void setChangeable(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    private void createOrUpdate(List<ResourceDiff> diffs, Map<String, BeamConfig> stateMap) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    execute(change, stateMap);
                }

                createOrUpdate(change.getDiffs(), stateMap);
            }
        }
    }

    private void delete(List<ResourceDiff> diffs, Map<String, BeamConfig> stateMap) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                delete(change.getDiffs(), stateMap);

                if (change.getType() == ChangeType.DELETE) {
                    execute(change, stateMap);
                }
            }
        }
    }

    private void execute(ResourceChange change, Map<String, BeamConfig> stateMap) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE ||
                change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d, stateMap);
            }
        }

        Beam.ui().write("Executing: ");
        writeChange(change);
        BeamResource resource = change.executeChange();
        BeamContextKey key = new BeamContextKey(resource.getResourceIdentifier(), resource.getType());

        String path;
        BeamConfig state;
        if (type == ChangeType.DELETE) {
            path = change.getCurrentResource().getPath();
            state = stateMap.get(path);
            state.removeReferable(key);
        } else {
            path = change.getPendingResource().getPath();
            if (!stateMap.containsKey(path)) {
                stateMap.put(path, new BeamConfig());
            }

            state = stateMap.get(path);
            state.addReferable(key, resource);
        }

        if (pendingStates.containsKey(path)) {
            state.importContext(pendingStates.get(path));
            pendingStates.remove(path);
        }

        Beam.ui().write(" OK\n");
        stateBackend.save(path, state);
    }
}
