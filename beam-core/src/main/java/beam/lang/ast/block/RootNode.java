package beam.lang.ast.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import beam.lang.Workflow;
import beam.lang.ast.DeferError;
import beam.lang.ast.ImportNode;
import beam.lang.ast.KeyValueNode;
import beam.lang.ast.Node;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

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
        List<KeyBlockNode> workflowNodes = new ArrayList<>();
        List<Node> body = new ArrayList<>();

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
                if (node instanceof KeyBlockNode) {
                    KeyBlockNode kbNode = (KeyBlockNode) node;

                    if (kbNode.getKey().equals("workflow")) {
                        workflowNodes.add(kbNode);
                        continue;
                    }
                }

                body.add(node);
            }
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

        for (KeyBlockNode wn : workflowNodes) {
            workflows.add(new Workflow(rootScope, wn));
        }

        DeferError.evaluate(scope, body);
        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildBody(builder, indentDepth, body);
    }

}
