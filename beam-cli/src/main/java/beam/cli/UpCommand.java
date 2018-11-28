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
import beam.lang.BeamConfigKey;
import beam.lang.BeamResolvable;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;

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

    private final Set<ChangeType> changeTypes = new HashSet<>();

    private BeamState stateBackend;

    @Override
    public void doExecute() throws Exception {

        if (getArguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        try {
            BCL.init();

            BeamConfig root = BeamCore.processConfig(getArguments().get(0));

            Set<BeamResource> resources = new TreeSet<>();
            Map<BeamConfigKey, BeamResolvable> pendingState = new HashMap<>();
            for (BeamConfigKey key : root.getContext().keySet()) {
                BeamResolvable resolvable = root.getContext().get(key);
                Object value = resolvable.getValue();

                if (value instanceof BeamResource) {
                    BeamResource resource = (BeamResource) value;
                    resource.setRoot(root);
                    resources.add(resource);
                } else if (value instanceof BeamState) {
                    stateBackend = (BeamState) value;
                } else {
                    pendingState.put(key, resolvable);
                }
            }

            if (stateBackend == null) {
                stateBackend = new BeamLocalState();
            }

            BeamConfig state = stateBackend.load(getArguments().get(0) + ".state");

            Set<BeamResource> current = new TreeSet<>();
            for (BeamConfigKey key : state.getContext().keySet()) {
                BeamResolvable resolvable = state.getContext().get(key);
                Object value = resolvable.getValue();

                if (value instanceof BeamResource) {
                    BeamResource resource = (BeamResource) value;
                    resource.setRoot(state);
                    current.add(resource);
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
                    createOrUpdate(diffs, state);
                }
            }

            if (changeTypes.contains(ChangeType.DELETE)) {
                hasChanges = true;

                if (Beam.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to delete resources?")) {
                    Beam.ui().write("\n");
                    setChangeable(diffs);
                    delete(diffs, state);
                }
            }

            if (!hasChanges) {
                Beam.ui().write("\nNo changes.\n");
            }

            state.getContext().putAll(pendingState);
            stateBackend.save(getArguments().get(0) + ".state", state);

        } finally {
            BCL.shutdown();
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

    private void createOrUpdate(List<ResourceDiff> diffs, BeamConfig state) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    execute(change, state);
                }

                createOrUpdate(change.getDiffs(), state);
            }
        }
    }

    private void delete(List<ResourceDiff> diffs, BeamConfig state) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                delete(change.getDiffs(), state);

                if (change.getType() == ChangeType.DELETE) {
                    execute(change, state);
                }
            }
        }
    }

    private void execute(ResourceChange change, BeamConfig state) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE ||
                change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d, state);
            }
        }

        Beam.ui().write("Executing: ");
        writeChange(change);
        BeamResource resource = change.executeChange();
        BeamConfigKey key = new BeamConfigKey(resource.getType(), resource.getResourceIdentifier());

        if (type == ChangeType.DELETE) {
            state.getContext().remove(key);
        } else {
            state.getContext().put(key, resource);
        }

        Beam.ui().write(" OK\n");
    }
}
