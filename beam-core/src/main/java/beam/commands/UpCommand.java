package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamUI;
import beam.core.diff.Diff;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.State;
import io.airlift.airline.Command;

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

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to change resources?")) {
                State state = new State(pending);

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
