package beam.cli;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.lang.BeamLanguageException;
import beam.lang.RootNode;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

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
        BeamCore core = new BeamCore();
        RootNode pending;
        try {
            pending = core.parse(configPath);
        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        BeamCore.ui().write("\n@|bold,white Looking for changes...\n\n|@");
        List<ResourceDiff> diffs = core.diff(pending, !skipRefresh);
        BeamCore.ui().write("\n");

        Set<ChangeType> changeTypes = core.writeDiffs(diffs);

        boolean hasChanges = false;
        if (changeTypes.contains(ChangeType.CREATE) || changeTypes.contains(ChangeType.UPDATE)) {
            hasChanges = true;

            if (BeamCore.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to create and/or update resources?")) {
                BeamCore.ui().write("\n");
                core.setChangeable(diffs);
                createOrUpdate(core, diffs);
            }
        }

        if (changeTypes.contains(ChangeType.DELETE)) {
            hasChanges = true;

            if (BeamCore.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to delete resources?")) {
                BeamCore.ui().write("\n");
                core.setChangeable(diffs);
                delete(core, diffs);
            }
        }

        if (changeTypes.contains(ChangeType.REPLACE)) {
            BeamCore.ui().write("\n");

            hasChanges = true;
        }

        if (!hasChanges) {
            BeamCore.ui().write("@|bold,green No changes.|@\n\n");
        }

    }

    public List<String> getArguments() {
        return arguments;
    }

    private void createOrUpdate(BeamCore core, List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    core.execute(change);
                }

                createOrUpdate(core, change.getDiffs());
            }
        }
    }

    private void delete(BeamCore core, List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                delete(core, change.getDiffs());

                if (change.getType() == ChangeType.DELETE) {
                    core.execute(change);
                }
            }
        }
    }
}
