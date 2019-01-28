package beam.lang.ast;

import java.util.Map;
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
    public Object evaluate(Scope scope) {
        Map<String, beam.lang.Resource> resources = scope.getPendingResources();

        if (name != null) {
            String n = Optional.ofNullable(name.evaluate(scope))
                    .map(Object::toString)
                    .orElse(null);

            beam.lang.Resource resource = resources.get(n);

            if (resource != null) {
                if (attribute != null) {
                    return resource.get(attribute);

                } else {
                    return resource;
                }

            } else {
                throw new DeferError(this);
            }

        } else {
            Stream<beam.lang.Resource> s = resources.values()
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
