package gyro.lang.ast.block;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.lang.Resource;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.DiffableScope;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;

public class ResourceNode extends BlockNode {

    private final String type;
    private final Node nameNode;

    public ResourceNode(String type, Node nameNode, List<Node> body) {
        super(body);

        this.type = type;
        this.nameNode = nameNode;
    }

    public ResourceNode(GyroParser.ResourceContext context) {
        super(context.blockBody()
                .blockStatement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

        type = context.resourceType().getText();
        nameNode = Node.create(context.resourceName().getChild(0));
    }

    public String getType() {
        return type;
    }

    public Node getNameNode() {
        return nameNode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object evaluate(Scope scope) throws Exception {
        String name = (String) nameNode.evaluate(scope);
        String fullName = type + "::" + name;
        DiffableScope bodyScope = new DiffableScope(scope);

        // Initialize the bodyScope with the resource values from the current
        // state scope.
        Optional.ofNullable(scope.getRootScope().getCurrent())
                .map(s -> s.findResource(fullName))
                .ifPresent(r -> {
                    Set<String> configuredFields = r.configuredFields();

                    for (DiffableField f : DiffableType.getInstance(r.getClass()).getFields()) {

                        // Don't copy nested diffables since they're handled
                        // by the diff system.
                        if (Diffable.class.isAssignableFrom(f.getItemClass())) {
                            continue;
                        }

                        String key = f.getBeamName();

                        // Skip over fields that were previously configured
                        // so that their removals can be detected by the
                        // diff system.
                        if (configuredFields.contains(key)) {
                            continue;
                        }

                        bodyScope.put(key, f.getValue(r));
                    }
                });

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        // Copy values from another.
        Object another = bodyScope.remove("_extends");
        RootScope rootScope = scope.getRootScope();

        if (another instanceof String) {
            Resource anotherResource = rootScope.findResource(type + "::" + another);

            if (anotherResource == null) {
                throw new IllegalArgumentException(String.format(
                        "No resource named [%s]!",
                        another));
            }

            for (DiffableField field : DiffableType.getInstance(anotherResource.getClass()).getFields()) {
                bodyScope.putIfAbsent(field.getBeamName(), field.getValue(anotherResource));
            }

        } else if (another instanceof Map) {
            ((Map<String, Object>) another).forEach(bodyScope::putIfAbsent);
        }

        Class<? extends Resource> resourceClass = (Class<? extends Resource>) rootScope.getResourceClasses().get(type);

        if (resourceClass == null) {
            VirtualResourceNode vrNode = scope.getRootScope().getVirtualResourceNodes().get(type);

            if (vrNode != null) {
                vrNode.createResources(name, bodyScope);
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
        resource.initialize(another != null ? new LinkedHashMap<>(bodyScope) : bodyScope);
        scope.getFileScope().put(fullName, resource);

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
