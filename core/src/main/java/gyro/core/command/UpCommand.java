package gyro.core.command;

import java.util.Set;

import gyro.core.GyroCore;
import gyro.core.GyroUI;
import gyro.core.resource.Diff;
import gyro.core.resource.RootScope;
import gyro.core.resource.State;
import io.airlift.airline.Command;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractConfigCommand {

    @Override
    public void doExecute(RootScope current, RootScope pending, State state) {
        GyroUI ui = GyroCore.ui();

        ui.write("\n@|bold,white Looking for changes...\n\n|@");

        Set<String> diffFiles = state.getDiffFiles();

        Diff diff = new Diff(
            current.findResourcesIn(diffFiles),
            pending.findResourcesIn(diffFiles));

        diff.diff();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to change resources?")) {
                ui.write("\n");
                diff.execute(ui, state);
            }

        } else {
            ui.write("\n@|bold,green No changes.|@\n\n");
        }
    }

}
