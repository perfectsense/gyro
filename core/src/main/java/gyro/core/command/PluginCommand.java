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
import gyro.parser.antlr4.GyroParser;
import gyro.util.Bug;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Command(name = "plugin", description = "Add or remove Gyro plugins.")
public class PluginCommand extends AbstractCommand {

    @Option(name = { "--add" })
    public boolean add;

    @Option(name = { "--remove" })
    public boolean remove;

    @Arguments(description = "A list of plugins specified in the format of <group>:<artifact>:<version>. "
        + "For example: gyro:gyro-aws-provider:0.1-SNAPSHOT")
    private List<String> plugins;

    @Override
    protected void doExecute() throws Exception {
        if (plugins == null || plugins.isEmpty()) {
            throw new GyroException("List of plugins is required!");
        }

        for (String plugin : plugins) {
            if (plugin.split(":").length != 3) {
                throw new GyroException(String.format(
                    "[%s] isn't properly formatted!",
                    plugin));
            }
        }

        if (add && remove) {
            throw new GyroException("Can't add and remove plugin at the same time!");
        }

        if (GyroCore.getRootDirectory() == null) {
            throw new GyroException("Can't find gyro root directory!");
        }

        FileBackend backend = new LocalFileBackend(GyroCore.getRootDirectory());
        Node node;
        try (GyroInputStream input = new GyroInputStream(backend, GyroCore.INIT_FILE)) {
            node = Node.parse(input, GyroCore.INIT_FILE, GyroParser::file);

        } catch (IOException error) {
            throw new Bug(error);
        }

        List<DirectiveNode> pluginNodes = new ArrayList<>();
        PluginNodeVisitor visitor = new PluginNodeVisitor();
        visitor.visit(node, pluginNodes);

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
        Path file = GyroCore.getRootDirectory().resolve(GyroCore.INIT_FILE);
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            int lineNumber = 0;
            while (line != null) {
                boolean skipLine = false;
                if (remove) {
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

        if (add) {
            plugins.forEach(p -> sb.append(String.format("%s '%s'%n", "@plugin:", p)));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
    }

}