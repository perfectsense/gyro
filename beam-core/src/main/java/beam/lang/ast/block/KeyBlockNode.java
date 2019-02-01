package beam.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
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

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        scope.addValue(key, bodyScope);

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