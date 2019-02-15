package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamUI;
import beam.core.diff.ChangeType;
import beam.core.diff.Diff;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.State;
import io.airlift.airline.Command;

import java.util.Set;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractConfigCommand {

    @Override
    public void doExecute(RootScope current, RootScope pending) throws Exception {
        BeamUI ui = BeamCore.ui();

        ui.write("\n@|bold,white Looking for changes...\n\n|@");

        Diff diff = new Diff(
                current.findAllResources(),
                pending.findAllResources());

        diff.diff();

        Set<ChangeType> changeTypes = diff.write(ui);
        State state = new State(pending);

        boolean hasChanges = false;

        if (changeTypes.contains(ChangeType.CREATE)
                || changeTypes.contains(ChangeType.UPDATE)) {

            hasChanges = true;

            if (ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to create and/or update resources?")) {
                ui.write("\n");
                diff.executeCreateOrUpdate(ui, state);
            }
        }

        if (changeTypes.contains(ChangeType.DELETE)) {
            hasChanges = true;

            if (ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to delete resources?")) {
                ui.write("\n");
                diff.executeDelete(ui, state);
            }
        }

        if (changeTypes.contains(ChangeType.REPLACE)) {
            ui.write("\n");

            hasChanges = true;
        }

        if (!hasChanges) {
            ui.write("\n@|bold,green No changes.|@\n\n");
        }
    }

}
