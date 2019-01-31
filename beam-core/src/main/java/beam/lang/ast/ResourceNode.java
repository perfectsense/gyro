package beam.lang.ast;

import beam.lang.BeamLanguageException;
import beam.lang.Resource;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourceNode extends Node {

    private final String type;
    private final Node nameNode;
    private final List<Node> body;

    public ResourceNode(BeamParser.ResourceContext context) {
        type = context.resourceType().IDENTIFIER().getText();
        nameNode = Node.create(context.resourceName().getChild(0));

        body = context.resourceBody()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) {
        String name = (String) nameNode.evaluate(scope);
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        // Find subresources.
        for (Iterator<Map.Entry<String, Object>> i = bodyScope.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Object> entry = i.next();
            String subresourceType = type + "::" + entry.getKey();

            if (scope.getTypes().get(subresourceType) != null) {
                Object value = entry.getValue();

                if (!(value instanceof List)) {
                    throw new IllegalArgumentException();
                }

                List<Resource> subresources = (List<Resource>) scope.computeIfAbsent("_subresources", s -> new ArrayList<>());

                for (Scope subresourceScope : (List<Scope>) value) {
                    Resource resource = createResource(scope, subresourceType);
                    resource.resourceType(subresourceType);
                    resource.scope(subresourceScope);
                    resource.syncInternalToProperties();

                    subresources.add(resource);
                }

                i.remove();
            }
        }

        Resource resource = createResource(scope, type);

        resource.resourceIdentifier(name);
        resource.scope(bodyScope);
        resource.syncInternalToProperties();
        scope.getResources().put(name, resource);

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append(type);

        if (nameNode != null) {
            builder.append(' ');
            builder.append(nameNode);
        }

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }

    private Resource createResource(Scope scope, String type) {
        Class klass = scope.getTypes().get(type);
        if (klass != null) {
            try {
                beam.lang.Resource resource = (beam.lang.Resource) klass.newInstance();
                resource.resourceType(type);

                return resource;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLanguageException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        throw new BeamLanguageException("Unknown resource type: " + type);
    }
}
