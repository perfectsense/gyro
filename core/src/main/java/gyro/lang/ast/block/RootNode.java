package gyro.lang.ast.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gyro.lang.Workflow;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.DirectiveNode;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class RootNode extends BlockNode {

    public RootNode(GyroParser.RootContext context) {
        super(context.statement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {

        // Evaluate imports and plugins first.
        List<DirectiveNode> imports = new ArrayList<>();
        List<PairNode> keyValues = new ArrayList<>();
        List<PluginNode> plugins = new ArrayList<>();
        Map<String, VirtualResourceNode> virtualResourceNodes = scope.getRootScope().getVirtualResourceNodes();
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

        for (DirectiveNode i : imports) {
            i.load(scope);
        }

        for (PairNode kv : keyValues) {
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
