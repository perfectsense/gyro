package gyro.lang.ast.block;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gyro.core.BeamCore;
import gyro.lang.BeamLanguageException;
import gyro.lang.Workflow;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.ImportNode;
import gyro.lang.ast.KeyValueNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

public class RootNode extends BlockNode {

    public RootNode(BeamParser.BeamFileContext context) {
        super(context.file()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {

        // Evaluate imports and plugins first.
        List<ImportNode> imports = new ArrayList<>();
        List<KeyValueNode> keyValues = new ArrayList<>();
        List<PluginNode> plugins = new ArrayList<>();
        Map<String, VirtualResourceNode> virtualResourceNodes = scope.getRootScope().getVirtualResourceNodes();
        List<ResourceNode> workflowNodes = new ArrayList<>();
        List<Node> body = new ArrayList<>();

        File rootConfig = new File(scope.getFileScope().getFile());
        Path configPath = Paths.get(rootConfig.getCanonicalPath());
        Path pluginPath = BeamCore.findPluginConfigPath(configPath);

        if (configPath.toString().endsWith(".gyro") && !Files.isSameFile(pluginPath, configPath)) {
            ImportNode pluginImport = new ImportNode(pluginPath.toString(), "_");
            imports.add(pluginImport);
        }

        for (Node node : this.body) {
            if (node instanceof ImportNode) {
                imports.add((ImportNode) node);

            } else if (node instanceof KeyValueNode) {
                keyValues.add((KeyValueNode) node);

            } else if (node instanceof PluginNode) {
                plugins.add((PluginNode) node);

            } else if (node instanceof VirtualResourceNode) {
                VirtualResourceNode vrNode = (VirtualResourceNode) node;
                virtualResourceNodes.put(vrNode.getName(), vrNode);

            } else {
                if (node instanceof ResourceNode) {
                    ResourceNode rnNode = (ResourceNode) node;

                    if (rnNode.getType().equals("workflow")) {
                        workflowNodes.add(rnNode);
                        continue;
                    }
                }

                body.add(node);
            }
        }

        if (configPath.endsWith(".gyro") && !Files.isSameFile(pluginPath, configPath) && !plugins.isEmpty()) {
            throw new BeamLanguageException(String.format("Plugins are only allowed to be defined in '%s', found in '%s'.", pluginPath, configPath));
        }

        for (ImportNode i : imports) {
            i.load(scope);
        }

        for (KeyValueNode kv : keyValues) {
            kv.evaluate(scope);
        }

        for (PluginNode plugin : plugins) {
            plugin.load(scope);
        }

        RootScope rootScope = scope.getRootScope();
        List<Workflow> workflows = rootScope.getWorkflows();

        for (ResourceNode rn : workflowNodes) {
            workflows.add(new Workflow(rootScope, rn));
        }

        DeferError.evaluate(scope, body);
        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildBody(builder, indentDepth, body);
    }

}
