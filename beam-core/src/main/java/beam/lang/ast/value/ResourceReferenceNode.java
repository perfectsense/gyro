package beam.lang.ast.value;

import beam.lang.Resource;
import beam.lang.ast.DeferError;
import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceReferenceNode extends Node {

    private final String type;
    private final Node name;
    private final String attribute;

    public ResourceReferenceNode(String type, Node name, String attribute) {
        this.type = type;
        this.name = name;
        this.attribute = attribute;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if (name != null) {
            String n = Optional.ofNullable(name.evaluate(scope))
                    .map(Object::toString)
                    .orElse(null);

            Object value = scope.find(n);

            if (value == null) {
                throw new DeferError(this);

            } else if (!(value instanceof Resource)) {
                throw new IllegalArgumentException(String.format(
                        "Expected the value named [%s] to be a resource but is an instance of [%s] instead!",
                        n, value.getClass().getName()));
            }

            Resource resource = (Resource) value;
            String resourceType = resource.resourceType();

            if (!type.equals(resourceType)) {
                throw new IllegalArgumentException(String.format(
                        "Expected the resource named [%s] to be of [%s] type but is of [%s] type instead!",
                        n, type, resourceType));
            }

            if (attribute != null) {
                return resource.get(attribute);

            } else {
                return value;
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

        if (name != null) {
            builder.append(' ');
            builder.append(name);
        }

        if (attribute != null) {
            builder.append(" | ");
            builder.append(attribute);
        }

        builder.append(")");
    }
}
