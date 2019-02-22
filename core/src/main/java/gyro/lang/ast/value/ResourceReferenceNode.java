package gyro.lang.ast.value;

import gyro.core.BeamException;
import gyro.lang.Resource;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.query.QueryExpressionNode;
import gyro.lang.ast.scope.DiffableScope;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ResourceQuery;
import gyro.parser.antlr4.BeamParser;
import gyro.parser.antlr4.BeamParser.ReferenceBodyContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceReferenceNode extends Node {

    private final String type;
    private final Node nameNode;
    private final String attribute;
    private final List<Node> filters;

    public ResourceReferenceNode(ReferenceBodyContext context) {
        type = context.referenceType().getText();

        BeamParser.ReferenceNameContext rnc = context.referenceName();
        if (rnc != null) {
            BeamParser.StringExpressionContext sec = rnc.stringExpression();

            if (sec != null) {
                nameNode = Node.create(sec);

            } else {
                nameNode = new StringNode(rnc.getText());
            }

        } else {
            nameNode = null;
        }

        filters = context.queryExpression()
            .stream()
            .map(f -> Node.create(f))
            .collect(Collectors.toList());

        String attributeValue = null;
        if (filters.size() > 0) {
            Node last = filters.get(filters.size() - 1);

            if (last instanceof AttributeNode) {
                try {
                    attributeValue = (String) last.evaluate(null);
                    filters.remove(filters.size() - 1);
                } catch (Exception ex) {
                    // Should not throw exception
                }
            }
        }

        this.attribute = attributeValue;
    }

    public ResourceReferenceNode(String type, Node nameNode, String attribute) {
        this.type = type;
        this.nameNode = nameNode;
        this.attribute = attribute;
        this.filters = Collections.EMPTY_LIST;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if (nameNode != null) {
            String name = (String) nameNode.evaluate(scope);

            if (name.startsWith("EXTERNAL/*")) {
                for (Node filter : filters) {
                    // TODO: first filter triggers api calls, subsequent fitlers use
                    //       the output from the first and narrow it down
                    Resource queryResource = queryResource(scope);
                    if (queryResource == null) {
                        throw new BeamException("Resource type " + type + " does not support external queries.");
                    }

                    ((QueryExpressionNode) filter).setResource(queryResource);
                    Object value = filter.evaluate(scope);

                    return value;
                }

                return null;
            } else {
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

    private Resource queryResource(Scope scope) throws Exception {

        @SuppressWarnings("unchecked")
        Class<? extends Resource> resourceClass = (Class<? extends Resource>) scope.getRootScope().getResourceClasses().get(type);

        Resource resource;

        try {
            resource = resourceClass.getConstructor().newInstance();

            resource.resourceType(type);
            resource.scope(new DiffableScope(scope));
            resource.initialize(scope);

            if (resource instanceof ResourceQuery) {
                return resource;
            }

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

        return null;
    }

}
