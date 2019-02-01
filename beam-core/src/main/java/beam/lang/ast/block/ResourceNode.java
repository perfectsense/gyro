package beam.lang.ast.block;

import beam.lang.BeamLanguageException;
import beam.lang.Resource;
import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;
import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourceNode extends BlockNode {

    private final String type;
    private final Node nameNode;

    public ResourceNode(String type, Node nameNode, List<Node> body) {
        super(body);

        this.type = type;
        this.nameNode = nameNode;
    }

    public ResourceNode(BeamParser.ResourceContext context) {
        super(context.resourceBody()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

        type = context.resourceType().IDENTIFIER().getText();
        nameNode = Node.create(context.resourceName().getChild(0));
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        String name = (String) nameNode.evaluate(scope);
        Scope bodyScope = new Scope(scope);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        Resource resource = createResource(scope, type);

        resource.resourceIdentifier(name);
        resource.scope(bodyScope);

        // Find subresources.
        for (Map.Entry<String, Object> entry : bodyScope.entrySet()) {
            String subresourceType = type + "::" + entry.getKey();

            if (scope.getRootScope().getTypeClasses().get(subresourceType) != null) {
                Object value = entry.getValue();

                if (!(value instanceof List)) {
                    throw new IllegalArgumentException();
                }

                List<Resource> subresources = new ArrayList<>();

                for (Scope subresourceScope : (List<Scope>) value) {
                    Resource subresource = createResource(scope, subresourceType);
                    subresource.parent(resource);
                    subresource.resourceType(entry.getKey());
                    subresource.scope(subresourceScope);
                    subresource.syncInternalToProperties();

                    subresources.add(subresource);
                }

                entry.setValue(subresources);
            }
        }

        resource.syncInternalToProperties();
        scope.getFileScope().getResources().put(name, resource);

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
        Class klass = scope.getRootScope().getTypeClasses().get(type);
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
