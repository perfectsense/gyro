package gyro.core.command;

import gyro.core.GyroCore;
import gyro.core.GyroUI;
import gyro.core.diff.Diff;
import gyro.core.scope.RootScope;
import gyro.core.scope.State;
import io.airlift.airline.Command;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractConfigCommand {

    @Override
    public void doExecute(RootScope current, RootScope pending, State state) throws Exception {
        GyroUI ui = GyroCore.ui();

        ui.write("\n@|bold,white Looking for changes...\n\n|@");

        Diff diff = new Diff(
                current.findAllResources(),
                pending.findAllResources());

        diff.diff();
        diff.validate();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to change resources?")) {
                ui.write("\n");
                diff.executeCreateOrUpdate(ui, state);
                diff.executeReplace(ui, state);
                diff.executeDelete(ui, state);
            }

        } else {
            ui.write("\n@|bold,green No changes.|@\n\n");
        }
    }

}
