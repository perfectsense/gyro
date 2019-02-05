package beam.commands;

import beam.core.BeamCore;
import beam.core.diff.ChangeType;
import beam.core.diff.Diff;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.State;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.util.Set;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractConfigCommand {

    @Option(name = { "--skip-refresh" })
    public boolean skipRefresh;

    @Override
    public void doExecute(RootScope current, RootScope pending) throws Exception {
        BeamCore.ui().write("\n@|bold,white Looking for changes...\n\n|@");

        Diff diff = new Diff(
                current.findAllResources(),
                pending.findAllResources());

        diff.setRefresh(!skipRefresh);
        diff.diff();

        Set<ChangeType> changeTypes = diff.write();
        State state = new State(pending);

        boolean hasChanges = false;

        if (changeTypes.contains(ChangeType.CREATE)
                || changeTypes.contains(ChangeType.UPDATE)) {

            hasChanges = true;

            if (BeamCore.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to create and/or update resources?")) {
                BeamCore.ui().write("\n");
                diff.executeCreateOrUpdate(state);
            }
        }

        if (changeTypes.contains(ChangeType.DELETE)) {
            hasChanges = true;

            if (BeamCore.ui().readBoolean(Boolean.FALSE, "\nAre you sure you want to delete resources?")) {
                BeamCore.ui().write("\n");
                diff.executeDelete(state);
            }
        }

        if (changeTypes.contains(ChangeType.REPLACE)) {
            BeamCore.ui().write("\n");

            hasChanges = true;
        }

        if (!hasChanges) {
            BeamCore.ui().write("\n@|bold,green No changes.|@\n\n");
        }
    }

}
