package beam.lang.ast.block;

import beam.lang.ast.DeferError;
import beam.lang.ast.ImportNode;
import beam.lang.ast.KeyValueNode;
import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<Node> body = new ArrayList<>();

        for (Node node : this.body) {
            if (node instanceof ImportNode) {
                imports.add((ImportNode) node);

            } else if (node instanceof KeyValueNode) {
                keyValues.add((KeyValueNode) node);

            } else if (node instanceof PluginNode) {
                plugins.add((PluginNode) node);

            } else {
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

        // Then the rest of the body until everything succeeds.
        int bodySize = body.size();

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            List<Node> deferred = new ArrayList<>();

            for (Node node : body) {
                try {
                    node.evaluate(scope);

                } catch (DeferError error) {
                    errors.add(error);
                    deferred.add(node);
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (bodySize == deferred.size()) {
                throw new RuntimeException(errors.toString());

            } else {
                body = deferred;
                bodySize = body.size();
            }
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildBody(builder, indentDepth, body);
    }
}
