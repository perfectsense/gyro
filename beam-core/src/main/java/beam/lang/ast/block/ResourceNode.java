package beam.lang.ast.block;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import beam.core.diff.DiffableField;
import beam.core.diff.DiffableType;
import beam.lang.Credentials;
import beam.lang.Resource;
import beam.lang.ast.Node;
import beam.lang.ast.scope.DiffableScope;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.scope.VirtualResourceScope;
import beam.parser.antlr4.BeamParser;

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
        DiffableScope bodyScope = new DiffableScope(scope);

        // Initialize the bodyScope with the resource values from the current
        // state scope.
        Optional.ofNullable(scope.getRootScope().getCurrent())
                .map(s -> s.findResource(name))
                .ifPresent(r -> {
                    for (DiffableField f : DiffableType.getInstance(r.getClass()).getFields()) {
                        if (!f.isSubresource()) {
                            bodyScope.put(f.getBeamName(), f.getValue(r));
                        }
                    }
                });

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        @SuppressWarnings("unchecked")
        Class<? extends Resource> resourceClass = (Class<? extends Resource>) scope.getRootScope().getResourceClasses().get(type);

        if (resourceClass == null) {
            VirtualResourceNode virtualResourceNode = scope.getRootScope().getVirtualResources().get(type);

            if (virtualResourceNode != null) {
                virtualResourceNode.evaluate(new VirtualResourceScope(bodyScope, name));
                return null;

            } else {
                throw new IllegalArgumentException(String.format(
                        "Can't create resource of [%s] type!",
                        type));
            }
        }

        Resource resource;

        try {
            resource = resourceClass.getConstructor().newInstance();

        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }

        resource.resourceType(type);
        resource.resourceIdentifier(name);
        resource.scope(bodyScope);
        resource.initialize(bodyScope);

        if (resource instanceof Credentials) {
            scope.getRootScope().getCredentialsMap().put(name, (Credentials) resource);

        } else {
            scope.getFileScope().getResources().put(name, resource);
        }

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

}
