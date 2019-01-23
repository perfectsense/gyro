package beam.lang.ast;

import beam.parser.antlr4.BeamParser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceNode extends Node {

    private final String type;
    private final Node name;
    private final List<Node> body;

    public ResourceNode(BeamParser.ResourceContext context) {
        type = context.resourceType().IDENTIFIER().getText();
        name = Node.create(context.resourceName().getChild(0));

        body = context.resourceBody()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }


    @Override
    public Object evaluate(Scope scope) {
        String n = Optional.ofNullable(name.evaluate(scope))
                .map(Object::toString)
                .orElse(null);

        if (n != null) {
            Scope bodyScope = new Scope(scope);

            for (Node node : body) {
                node.evaluate(bodyScope);
            }

            scope.getCurrentResources().put(n, new Resource(type, n, null));
            scope.getPendingResources().put(n, new Resource(type, n, bodyScope));
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append(type);
        builder.append(' ');
        builder.append(name);

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }
}
