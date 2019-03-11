package gyro.lang.ast.value;

import com.google.common.collect.ImmutableList;
import gyro.core.BeamException;
import gyro.lang.Resource;
import gyro.lang.ResourceFinder;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.query.FieldValueQuery;
import gyro.lang.ast.query.Query;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import gyro.parser.antlr4.BeamParser.ReferenceBodyContext;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceReferenceNode extends Node {

    private final String type;
    private final Node nameNode;
    private final List<Query> queries;
    private final String attribute;

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

        queries = context.queryExpression()
            .stream()
            .map(Query::create)
            .collect(Collectors.toList());

        String attributeValue = null;
        Integer size = queries.size();
        if (size > 0) {
            Query last = queries.get(size - 1);

            if (last instanceof FieldValueQuery) {
                attributeValue = ((FieldValueQuery) last).getValue();
                queries.remove(size - 1);
            }
        }

        this.attribute = attributeValue;
    }

    public ResourceReferenceNode(String type, Node nameNode, String attribute, Collection<Query> queries) {
        this.type = type;
        this.nameNode = nameNode;
        this.attribute = attribute;
        this.queries = ImmutableList.copyOf(queries);
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if (nameNode != null) {
            String name = (String) nameNode.evaluate(scope);

            List<Resource> resources;
            if (name.startsWith("EXTERNAL/*")) {
                Class<? extends ResourceFinder> resourceQueryClass = scope.getRootScope().getResourceFinderClasses().get(type);

                if (resourceQueryClass == null) {
                    throw new BeamException("Resource type " + type + " does not support external queries.");
                }

                ResourceFinder<Resource> finder = resourceQueryClass.getConstructor().newInstance();
                finder.credentials(finder.resourceCredentials(scope));
                resources = finder.findAll();

            } else if (name.endsWith("*")) {
                RootScope rootScope = scope.getRootScope();
                Stream<Resource> s = rootScope.findAllResources().stream().filter(r -> type.equals(r.resourceType()));

                if (!name.equals("*")) {
                    String prefix = name.substring(0, name.length() - 1);
                    s = s.filter(r -> r.resourceIdentifier().startsWith(prefix));
                }

                resources = s.collect(Collectors.toList());

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

            for (Query q : queries) {
                resources = q.evaluate(type, scope, resources);
            }

            if (attribute != null) {
                return resources.stream().map(r -> r.get(attribute)).collect(Collectors.toList());
            } else {
                return resources;
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
