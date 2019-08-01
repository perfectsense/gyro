package gyro.core.command;

import java.util.Arrays;
import java.util.List;

public class PluginCommandGroup implements GyroCommandGroup {

    @Override
    public String getName() {
        return "plugin";
    }

    @Override
    public String getDescription() {
        return "Add, remove, or list plugins.";
    }

    @Override
    public List<Class<?>> getCommands() {
        return Arrays.asList(PluginAddCommand.class, PluginRemoveCommand.class, PluginListCommand.class);
    }

    @Override
    public Class<?> getDefaultCommand() {
        return PluginHelp.class;
    }

}
