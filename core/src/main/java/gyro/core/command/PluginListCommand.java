package gyro.core.command;

import gyro.core.GyroCore;

import io.airlift.airline.Command;

@Command(name = "list", description = "List Gyro plugins.")
public class PluginListCommand extends PluginCommand {

    @Override
    protected void executeSubCommand() {
        getPluginNodes()
            .stream()
            .map(this::toPluginString)
            .map(p -> String.format("@|bold %s|@%n", p))
            .forEach(GyroCore.ui()::write);
    }

}
