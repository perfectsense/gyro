package gyro.core.command;

import gyro.core.GyroCore;

import io.airlift.airline.Command;

@Command(name = "list", description = "List installed plugins.")
public class PluginListCommand extends PluginCommand {

    @Override
    protected void executeSubCommand() {
        GyroCore.ui().write("\n");

        getPluginNodes()
            .stream()
            .map(this::toPluginString)
            .map(p -> String.format("@|bold %s|@%n", p))
            .forEach(GyroCore.ui()::write);
    }

}
