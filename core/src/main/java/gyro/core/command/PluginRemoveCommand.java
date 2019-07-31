package gyro.core.command;

import gyro.core.GyroException;
import gyro.lang.ast.block.DirectiveNode;
import io.airlift.airline.Command;

import java.util.List;
import java.util.stream.Collectors;

@Command(name = "remove", description = "Remove Gyro plugins.")
public class PluginRemoveCommand extends PluginCommand {

    @Override
    protected void executeSubCommand() throws Exception {
        if (getPlugins().isEmpty()) {
            throw new GyroException("List of plugins is required!");
        }

        List<DirectiveNode> removeNodes = getPluginNodes()
            .stream()
            .filter(this::pluginNodeExist)
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        int lineNumber = 0;
        for (String line : load()) {
            boolean skipLine = false;
            for (DirectiveNode pluginNode : removeNodes) {
                if (lineNumber >= pluginNode.getStartLine() && lineNumber <= pluginNode.getStopLine()) {
                    skipLine = true;
                }
            }

            if (!skipLine) {
                sb.append(line);
                sb.append("\n");
            }

            lineNumber++;
        }

        save(sb.toString());
    }

}
