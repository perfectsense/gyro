package beam.cli;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.BeamState;
import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.lang.BeamLanguageException;
import beam.lang.types.ContainerBlock;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.fusesource.jansi.AnsiRenderer;

import java.util.List;
import java.util.Set;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    @Option(name = { "--skip-refresh" })
    public boolean skipRefresh;

    @Override
    public void doExecute() throws Exception {

        if (getArguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        String configPath = getArguments().get(0);
        String statePath = configPath + ".state";
        BeamCore core = new BeamCore();
        ContainerBlock rootBlock = null;
        try {
            rootBlock = core.parse(configPath);
        } catch (BeamLanguageException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        BeamState backend = core.getState(rootBlock);
        ContainerBlock state = backend.load(statePath, core);
        core.copyNonResourceState(rootBlock, state);

        if (BeamCore.validationException().isThrowing()) {
            throw BeamCore.validationException();
        }

        Set<BeamResource> resources = core.findBeamResources(rootBlock);
        Set<BeamResource> current = core.findBeamResources(state, !skipRefresh);

        BeamCore.ui().write("@|bold,blue [beam]:|@ Looking for changes...");
        List<ResourceDiff> diffs = core.diff(current, resources);
        BeamCore.ui().write("\n\n");

        Set<ChangeType> changeTypes = core.writeDiffs(diffs);

        boolean hasChanges = false;
        if (changeTypes.contains(ChangeType.CREATE) || changeTypes.contains(ChangeType.UPDATE)) {
            hasChanges = true;

            if (BeamCore.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to create and/or update resources?")) {
                BeamCore.ui().write("\n");
                core.setChangeable(diffs);
                createOrUpdate(core, diffs, state, backend, statePath);
            }
        }

        if (changeTypes.contains(ChangeType.DELETE)) {
            hasChanges = true;

            if (BeamCore.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to delete resources?")) {
                BeamCore.ui().write("\n");
                core.setChangeable(diffs);
                delete(core, diffs, state, backend, statePath);
            }
        }

        if (!hasChanges) {
            BeamCore.ui().write("@|bold,green No changes.|@\n");
        }

    }

    public List<String> getArguments() {
        return arguments;
    }

    private void createOrUpdate(BeamCore core, List<ResourceDiff> diffs, ContainerBlock state, BeamState stateBackend, String path) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    core.execute(change, state, stateBackend, path);
                }

                createOrUpdate(core, change.getDiffs(), state, stateBackend, path);
            }
        }
    }

    private void delete(BeamCore core, List<ResourceDiff> diffs, ContainerBlock state, BeamState stateBackend, String path) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                delete(core, change.getDiffs(), state, stateBackend, path);

                if (change.getType() == ChangeType.DELETE) {
                    core.execute(change, state, stateBackend, path);
                }
            }
        }
    }
}
