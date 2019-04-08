package gyro.commands;

import gyro.core.BeamCore;
import gyro.core.BeamUI;
import gyro.core.diff.Diff;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.State;
import io.airlift.airline.Command;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractConfigCommand {

    @Override
    public void doExecute(RootScope current, RootScope pending, State state) throws Exception {
        BeamUI ui = BeamCore.ui();

        ui.write("\n@|bold,white Looking for changes...\n\n|@");

        Diff diff = new Diff(
                current.findAllResources(),
                pending.findAllResources());

        diff.diff();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to change resources?")) {
                ui.write("\n");
                diff.executeCreateOrUpdate(ui, state);
                diff.executeReplace(ui, state);
                diff.executeDelete(ui, state);
                state.cleanUp();
            }

        } else {
            ui.write("\n@|bold,green No changes.|@\n\n");
        }
    }

}
