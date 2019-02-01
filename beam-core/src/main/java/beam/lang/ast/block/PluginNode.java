package beam.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.lang.plugins.PluginLoader;
import beam.parser.antlr4.BeamParser;

public class PluginNode extends BlockNode {

    public PluginNode(List<Node> body) {
        super(body);
    }

    public PluginNode(BeamParser.ResourceContext context) {
        super(context.resourceBody()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));
    }

    public void load(Scope scope) throws Exception {
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        PluginLoader loader = new PluginLoader(bodyScope);

        loader.load();
        scope.getFileScope().getPluginLoaders().add(loader);
    }

    @Override
    public Object evaluate(Scope scope) {
        throw new IllegalArgumentException();
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append("plugin");

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }

}
