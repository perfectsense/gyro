package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.lang.BeamInterp;
import beam.lang.types.ContainerBlock;
import beam.lang.types.ResourceBlock;
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

    public ContainerBlock parse(String path) throws IOException {
        interp.init();
        interp.addExtension("state", BeamLocalState.class);
        interp.addExtension("provider", BeamProvider.class);

        ContainerBlock block = interp.parse(path);

        return block;
    }

    public BeamState getState(ContainerBlock block) {
        BeamState backend = new BeamLocalState();

        for (ResourceBlock resourceBlock : block.resources()) {
            if (resourceBlock instanceof BeamState) {
                backend = (BeamState) resourceBlock;
            }
        }

        return backend;
    }

    public void copyNonResourceState(ContainerBlock source, ContainerBlock state) {
        state.copyNonResourceState(source);

        for (ResourceBlock block : source.resources()) {
            if (block instanceof BeamResource) {
                continue;
            }

            state.putResource(block);
        }
    }

    public Set<BeamResource> findBeamResources(ContainerBlock block) {
        return findBeamResources(block, false);
    }

    public Set<BeamResource> findBeamResources(ContainerBlock block, boolean refresh) {
        Set<BeamResource> resources = new TreeSet<>();

        for (ResourceBlock resource : block.resources()) {
            if (resource instanceof BeamResource) {
                if (refresh) {
                    BeamCore.ui().write("@|bold,blue [beam]:|@ Refreshing @|yellow %s|@(%s)...", resource.getResourceType(), resource.getResourceIdentifier());
                }

                if (refresh && ((BeamResource) resource).refresh()) {
                    ((BeamResource) resource).syncPropertiesToInternal();

                    BeamCore.ui().write("\n");
                }

                resources.add((BeamResource) resource);
            }

            resources.addAll(findBeamResources(resource, refresh));
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

    public void execute(ResourceChange change, ContainerBlock state, BeamState backend, String path) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d, state, backend, path);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        BeamResource resource = change.executeChange();

        if (type == ChangeType.DELETE) {
            state.removeResource(resource);
        } else {
            state.putResource(resource);

        }

        BeamCore.ui().write(" OK\n");
        backend.save(path, state);
    }
}
