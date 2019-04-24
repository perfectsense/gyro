package gyro.lang.ast.block;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.scope.FileScope;
import gyro.core.workflow.Workflow;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.DirectiveNode;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class FileNode extends BlockNode {

    public FileNode(GyroParser.FileContext context, String file) {
        super(context.statement()
                .stream()
                .map(c -> Node.create(c.getChild(0), file))
                .collect(Collectors.toList()));
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        RootScope rootScope = scope.getRootScope();
        FileScope fileScope = null;
        if (rootScope.getFile().equals(getFile())) {
            fileScope = rootScope;
        } else {
            for (FileScope s : rootScope.getFileScopes()) {
                if (s.getFile().equals(getFile())) {
                    fileScope = s;
                }
            }
        }

        if (fileScope == null) {
            fileScope = new FileScope(rootScope, getFile());
            rootScope.getFileScopes().add(fileScope);
        }

        // Evaluate imports and plugins first.
        List<DirectiveNode> imports = new ArrayList<>();
        List<PairNode> keyValues = new ArrayList<>();
        List<PluginNode> plugins = new ArrayList<>();
        Map<String, VirtualResourceNode> virtualResourceNodes = rootScope.getVirtualResourceNodes();
        List<ResourceNode> workflowNodes = new ArrayList<>();
        List<Node> body = new ArrayList<>();

        for (Node node : this.body) {
            if (node instanceof DirectiveNode) {
                imports.add((DirectiveNode) node);

            } else if (node instanceof PairNode) {
                keyValues.add((PairNode) node);

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

        File rootConfig = new File(fileScope.getFile());
        Path configPath = Paths.get(rootConfig.getCanonicalPath());
        Path initFile = GyroCore.getRootInitFile();

        if (!plugins.isEmpty() && Files.exists(configPath) && !Files.isSameFile(initFile, configPath)) {
            throw new GyroException(String.format("Plugins are only allowed to be defined in '%s'.%nThe following plugins are found in '%s':%n%s",
                initFile,
                configPath,
                plugins.stream()
                    .map(Node::toString)
                    .collect(Collectors.joining("\n"))));
        }

        for (DirectiveNode i : imports) {
            i.load(fileScope);
        }

        for (PairNode kv : keyValues) {
            kv.evaluate(fileScope);
        }

        for (PluginNode plugin : plugins) {
            plugin.load(fileScope);
        }

        List<Workflow> workflows = rootScope.getWorkflows();

        for (ResourceNode rn : workflowNodes) {
            workflows.add(new Workflow(rootScope, rn));
        }

        DeferError.evaluate(fileScope, body);
        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildBody(builder, indentDepth, body);
    }

}
