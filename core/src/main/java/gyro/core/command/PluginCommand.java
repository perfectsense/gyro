package gyro.core.command;

import gyro.core.FileBackend;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.LocalFileBackend;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.parser.antlr4.GyroParser;
import gyro.util.Bug;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Command(name = "plugin", description = "Add or remove Gyro plugins.")
public class PluginCommand extends AbstractCommand {

    @Arguments(description = "add|remove plugins. A list of plugins specified in the format of <group>:<artifact>:<version>. "
        + "For example: add gyro:gyro-aws-provider:0.1-SNAPSHOT")
    private List<String> arguments;

    @Override
    protected void doExecute() throws Exception {
        if (GyroCore.getRootDirectory() == null) {
            throw new GyroException("Can't find gyro root directory!");
        }

        if (arguments == null || arguments.isEmpty()) {
            throw new GyroException("Expected 'gyro plugin add|remove plugins'!");
        }

        String command = arguments.get(0);
        if (!"add".equalsIgnoreCase(command) && !"remove".equalsIgnoreCase(command)) {
            throw new GyroException("Unknown command. Valid commands are: add and remove");
        }

        List<String> plugins = new ArrayList<>(arguments.subList(1, arguments.size()));
        if (plugins.isEmpty()) {
            throw new GyroException("List of plugins is required!");
        }

        for (String plugin : plugins) {
            if (plugin.split(":").length != 3) {
                throw new GyroException(String.format(
                    "@|bold %s|@ isn't properly formatted!",
                    plugin));
            }
        }

        FileBackend backend = new LocalFileBackend(GyroCore.getRootDirectory());
        List<DirectiveNode> pluginNodes;
        try (GyroInputStream input = new GyroInputStream(backend, GyroCore.INIT_FILE)) {
            pluginNodes = ((FileNode) Node.parse(input, GyroCore.INIT_FILE, GyroParser::file))
                .getBody()
                .stream()
                .filter(DirectiveNode.class::isInstance)
                .map(DirectiveNode.class::cast)
                .filter(n -> "plugin".equals(n.getName()))
                .collect(Collectors.toList());

        } catch (IOException error) {
            throw new Bug(error);
        }

        NodeEvaluator evaluator = new NodeEvaluator();
        Scope scope = new Scope(null);

        List<DirectiveNode> removeNodes = new ArrayList<>();
        Iterator<String> iter = plugins.iterator();
        while (iter.hasNext()) {
            String plugin = iter.next();
            for (DirectiveNode pluginNode : pluginNodes) {
                String existPlugin = (String) evaluator.visit(pluginNode.getArguments().get(0), scope);
                if (plugin.equals(existPlugin)) {
                    removeNodes.add(pluginNode);
                    iter.remove();
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(backend.openInput(GyroCore.INIT_FILE)))) {
            String line = reader.readLine();
            int lineNumber = 0;
            while (line != null) {
                boolean skipLine = false;
                if ("remove".equalsIgnoreCase(command)) {
                    for (DirectiveNode pluginNode : removeNodes) {
                        if (lineNumber >= pluginNode.getStartLine() && lineNumber <= pluginNode.getStopLine()) {
                            skipLine = true;
                        }
                    }
                }

                if (!skipLine) {
                    sb.append(line);
                    sb.append("\n");
                }

                line = reader.readLine();
                lineNumber++;
            }
        }

        if ("add".equalsIgnoreCase(command)) {
            plugins.forEach(p -> sb.append(String.format("%s '%s'%n", "@plugin:", p)));
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(backend.openOutput(GyroCore.INIT_FILE)))) {
            writer.write(sb.toString());
        }
    }

}