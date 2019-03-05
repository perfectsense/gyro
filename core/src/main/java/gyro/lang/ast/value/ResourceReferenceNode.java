package gyro.lang.ast.value;

import gyro.lang.Resource;
import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.query.FieldValueQuery;
import gyro.lang.ast.query.Query;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import gyro.parser.antlr4.BeamParser.ReferenceBodyContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceReferenceNode extends Node {

    private final String type;
    private final Node nameNode;
    private final String attribute;
    private final List<Query> queries;

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
            .map(f -> Query.create(f))
            .collect(Collectors.toList());

        String attributeValue = null;
        if (queries.size() > 0) {
            Query last = queries.get(queries.size() - 1);

            if (last instanceof FieldValueQuery) {
                attributeValue = ((FieldValueQuery) last).getValue();
                queries.remove(queries.size() - 1);
            }
        }

        this.attribute = attributeValue;
    }

    public ResourceReferenceNode(String type, Node nameNode, String attribute) {
        this.type = type;
        this.nameNode = nameNode;
        this.attribute = attribute;
        this.queries = Collections.EMPTY_LIST;
    }

    private List<ResourceQueryGroup> parseQueries(Scope scope, boolean external) throws Exception {
        List<ResourceQueryGroup> groups = new ArrayList<>();
        if (external) {
            ResourceQueryGroup group = new ResourceQueryGroup(Query.createExternalResourceQuery(scope, type));
            groups.add(group);
        }

        for (Query query : queries) {
            List<ResourceQueryGroup> groupsForOneQuery = query.evaluate(scope, type, external);
            if (groups.isEmpty()) {
                groups = groupsForOneQuery;
            } else {
                List<ResourceQueryGroup> result = new ArrayList<>();
                for (ResourceQueryGroup left : groups) {
                    for (ResourceQueryGroup right : groupsForOneQuery) {
                        ResourceQueryGroup joined = new ResourceQueryGroup(Query.createExternalResourceQuery(scope, type));
                        ResourceQueryGroup.join(left, right, joined);
                        result.add(joined);
                    }
                }

                groups = result;
            }
        }

        return groups;
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if (nameNode != null) {
            String name = (String) nameNode.evaluate(scope);

            List<Resource> resources = new ArrayList<>();
            if (name.startsWith("EXTERNAL/*")) {
                List<ResourceQueryGroup> groups = parseQueries(scope, true);

                for (ResourceQueryGroup group : groups) {
                    group.merge();
                    resources.addAll(group.query());
                }

                if (attribute != null) {
                    return resources.stream().map(r -> r.get(attribute)).collect(Collectors.toList());
                } else {
                    return resources;
                }

            } else if (name.endsWith("*")) {
                RootScope rootScope = scope.getRootScope();
                Stream<Resource> s = rootScope.findAllResources().stream().filter(r -> type.equals(r.resourceType()));

                if (!name.equals("*")) {
                    String prefix = name.substring(0, name.length() - 1);
                    s = s.filter(r -> r.resourceIdentifier().startsWith(prefix));
                }

                List<ResourceQueryGroup> groups = parseQueries(scope, false);
                List<Resource> baseResources = s.collect(Collectors.toList());

                if (queries.isEmpty()) {
                    resources.addAll(baseResources);
                } else {
                    for (ResourceQueryGroup group : groups) {
                        resources.addAll(group.query(baseResources));
                    }
                }

                if (attribute != null) {
                    return resources.stream().map(r -> r.get(attribute)).collect(Collectors.toList());
                } else {
                    return resources;
                }

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
}
