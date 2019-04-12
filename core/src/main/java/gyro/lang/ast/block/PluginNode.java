package gyro.lang.ast.block;

import gyro.lang.ast.Node;
import gyro.core.scope.Scope;
import gyro.lang.plugins.PluginLoader;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class PluginNode extends BlockNode {

    public PluginNode(List<Node> body) {
        super(body);
    }

    public PluginNode(GyroParser.ResourceContext context) {
        super(context.blockBody()
                .blockStatement()
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
