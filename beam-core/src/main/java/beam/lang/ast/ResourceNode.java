package beam.lang.ast;

import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceNode extends Node {

    private final String type;
    private final Node name;
    private final List<Node> body;

    public ResourceNode(BeamParser.ResourceContext context) {
        type = context.resourceType().IDENTIFIER().getText();

        if (context.resourceName() != null) {
            name = Node.create(context.resourceName().getChild(0));
        } else {
            name = null;
        }

        body = new ArrayList<>();
        for (BeamParser.ResourceBodyContext bodyContext : context.resourceBody()) {
            if (bodyContext.resource() != null) {
                body.add(new KeyListValueNode(bodyContext.resource()));
            } else {
                body.add(Node.create(bodyContext.getChild(0)));
            }
        }
    }


    @Override
    public Object evaluate(Scope scope) {
        String n = null;

        if (name != null) {
            n = Optional.ofNullable(name.evaluate(scope))
                .map(Object::toString)
                .orElse(null);
        }

        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        if (n != null) {
            scope.getCurrentResources().put(n, new Resource(type, n, null));
            scope.getPendingResources().put(n, new Resource(type, n, bodyScope));
        } else {
            if ("plugin".equals(type)) {
                scope.getPlugins().add(new Resource(type, n, bodyScope));
            }
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append(type);

        if (name != null) {
            builder.append(' ');
            builder.append(name);
        }

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }
}
