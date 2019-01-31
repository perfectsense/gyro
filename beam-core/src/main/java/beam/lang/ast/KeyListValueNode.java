package beam.lang.ast;

import java.util.List;
import java.util.stream.Collectors;

import beam.lang.ast.scope.Scope;
import beam.lang.plugins.PluginLoader;
import beam.parser.antlr4.BeamParser;

public class KeyListValueNode extends Node {

    private final String key;
    private final List<Node> body;

    public KeyListValueNode(BeamParser.ResourceContext context) {
        key = context.resourceType().IDENTIFIER().getText();

        body = context.resourceBody()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) {
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        if ("plugin".equals(key)) {
            PluginLoader loader = new PluginLoader(bodyScope);

            loader.load();
            scope.getProcessScope().getPluginLoaders().add(loader);

        } else if ("state".equals(key)) {
            //scope.setStateBackend(new Resource(type, n, bodyScope));

        } else {
            scope.addValue(key, bodyScope);
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