package gyro.lang.ast.value;

import gyro.lang.Resource;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceReferenceNode extends Node {

    private final String type;
    private final Node nameNode;
    private final String attribute;

    public ResourceReferenceNode(String type, Node nameNode, String attribute) {
        this.type = type;
        this.nameNode = nameNode;
        this.attribute = attribute;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if (nameNode != null) {
            String name = (String) nameNode.evaluate(scope);
            String fullName = type + "::" + name;
            Resource resource = scope.getRootScope().findResource(fullName);

            if (resource == null) {
                throw new DeferError(this);
            }

            if (attribute != null) {
                return resource.get(attribute);

            } else {
                return resource;
            }

        } else {
            Stream<Resource> s = scope.getRootScope()
                    .findAllResources()
                    .stream()
                    .filter(r -> type.equals(r.resourceType()));

            if (attribute != null) {
                return s.map(r -> r.get(attribute))
                        .collect(Collectors.toList());

            } else {
                return s.collect(Collectors.toList());
            }
        }
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append("$(");
        builder.append(type);

        if (nameNode != null) {
            builder.append(' ');
            builder.append(nameNode);
        }

        if (attribute != null) {
            builder.append(" | ");
            builder.append(attribute);
        }

        builder.append(")");
    }

}
