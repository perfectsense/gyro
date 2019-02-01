package beam.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.lang.plugins.PluginLoader;
import beam.parser.antlr4.BeamParser;

public class KeyBlockNode extends BlockNode {

    private final String key;

    public KeyBlockNode(String key, List<Node> body) {
        super(body);

        this.key = key;
    }

    public KeyBlockNode(BeamParser.ResourceContext context) {
        super(context.resourceBody()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

        key = context.resourceType().IDENTIFIER().getText();
    }

    public String getKey() {
        return key;
    }

    private Scope createBodyScope(Scope scope) throws Exception {
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        return bodyScope;
    }

    public void loadPlugin(Scope scope) throws Exception {
        PluginLoader loader = new PluginLoader(createBodyScope(scope));

        loader.load();
        scope.getFileScope().getPluginLoaders().add(loader);
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if ("plugin".equals(key)) {
            throw new IllegalArgumentException();

        } else if ("state".equals(key)) {
            throw new IllegalArgumentException();

        } else {
            scope.addValue(key, createBodyScope(scope));
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append(key);

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }

}