package gyro.core.command;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import io.airlift.airline.Command;

import java.util.Set;
import java.util.stream.Collectors;

@Command(name = "add", description = "Add Gyro plugins.")
public class PluginAddCommand extends PluginCommand {

    @Override
    protected void executeSubCommand() throws Exception {
        if (getPlugins().isEmpty()) {
            throw new GyroException("List of plugins is required!");
        }

        Set<String> plugins = getPlugins()
            .stream()
            .filter(this::pluginNotExist)
            .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        load()
            .stream()
            .map(l -> l + "\n")
            .forEach(sb::append);

        plugins.forEach(p -> sb.append(String.format("%s '%s'%n", "@plugin:", p)));
        save(sb.toString());

        plugins.stream()
            .map(p -> String.format("@|bold %s|@ has been added.%n", p))
            .forEach(GyroCore.ui()::write);
    }

}
