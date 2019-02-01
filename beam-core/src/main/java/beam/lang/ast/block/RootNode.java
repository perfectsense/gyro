package beam.lang.ast.block;

import beam.lang.ast.DeferError;
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
        List<Node> body = this.body;
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
