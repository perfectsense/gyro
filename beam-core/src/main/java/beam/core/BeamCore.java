package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.lang.BeamInterp;
import beam.lang.types.BeamBlock;
import beam.lang.types.ContainerBlock;
import com.psddev.dari.util.ThreadLocalStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class BeamCore {

    private static final ThreadLocalStack<BeamUI> UI = new ThreadLocalStack<>();

    private static final BeamValidationException validationException = new BeamValidationException("Invalid config!");

    private final BeamInterp interp = new BeamInterp();

    public static BeamUI ui() {
        return UI.get();
    }

    public static void pushUi(BeamUI ui) {
        UI.push(ui);
    }

    public static BeamUI popUi() {
        return UI.pop();
    }

    public static BeamValidationException validationException() {
        return validationException;
    }

    public BeamBlock parse(String path) throws IOException {
        interp.init();
        interp.addExtension("state", BeamLocalState.class);
        interp.addExtension("provider", BeamProvider.class);

        BeamBlock block = interp.parse(path);

        return block;
    }

    public BeamState getStateBackend(BeamBlock config) {
        BeamState stateBackend = new BeamLocalState();
        /*
        for (BeamContextKey key : config.keys()) {
            BeamReferable referable = config.get(key);
            Object value = referable.getValue();

            if (value instanceof BeamState) {
                stateBackend = (BeamState) value;
            }
        }
        */

        return stateBackend;
    }

    public BeamBlock findNonResources(BeamBlock config) {
        BeamBlock nonResourceConfig = new ContainerBlock();
        /*
        for (BeamContextKey key : config.keys()) {
            BeamReferable referable = config.get(key);
            Object value = referable.getValue();

            if (!(value instanceof BeamResource)) {
                nonResourceConfig.add(key, referable);
            }
        }
        */

        return nonResourceConfig;
    }

    public Set<BeamResource> findBeamResources(BeamBlock config) {
        return findBeamResources(config, false);
    }

    public Set<BeamResource> findBeamResources(BeamBlock block, boolean refresh) {
        Set<BeamResource> resources = new TreeSet<>();

        if (block instanceof ContainerBlock) {
            ContainerBlock containerBlock = (ContainerBlock) block;

            for (BeamBlock child : containerBlock.getBlocks()) {
                if (child instanceof BeamResource) {
                    resources.add((BeamResource) child);
                }

                resources.addAll(findBeamResources(child, refresh));
            }
        }

        return resources;
    }

    public List<ResourceDiff> diff(Set<BeamResource> current, Set<BeamResource> pending) throws Exception {
        ResourceDiff diff = new ResourceDiff(current, pending);
        diff.diff();

        List<ResourceDiff> diffs = new ArrayList<>();
        diffs.add(diff);

        return diffs;
    }

    public Set<ChangeType> writeDiffs(List<ResourceDiff> diffs) {
        Set<ChangeType> changeTypes = new HashSet<>();
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
                BeamCore.ui().write("\n");
                BeamCore.ui().indented(() -> changeTypes.addAll(writeDiffs(changeDiffs)));
            }
        }

        return changeTypes;
    }

    private void writeChange(ResourceChange change) {
        switch (change.getType()) {
            case CREATE :
                BeamCore.ui().write("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    BeamCore.ui().write(" * %s", change);
                } else {
                    BeamCore.ui().write("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                BeamCore.ui().write("@|blue * %s|@", change);
                break;

            case DELETE :
                BeamCore.ui().write("@|red - %s|@", change);
                break;

            default :
                BeamCore.ui().write(change.toString());
        }
    }

    public void setChangeable(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    public void execute(ResourceChange change, BeamBlock state, BeamState stateBackend, String path) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d, state, stateBackend, path);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        BeamResource resource = change.executeChange();

        /*
        BeamContextKey key = new BeamContextKey(resource.getResourceIdentifier(), resource.getResourceType());

        if (type == ChangeType.DELETE) {
            state.remove(key);
        } else {
            state.add(key, resource);
        }
        */

        BeamCore.ui().write(" OK\n");
        stateBackend.save(path, state);
    }
}
