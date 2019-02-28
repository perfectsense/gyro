package gyro.lang.ast.value;

import gyro.core.BeamException;
import gyro.lang.Resource;
import gyro.lang.ResourceQuery;
import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.query.FieldValueQuery;
import gyro.lang.ast.query.Query;
import gyro.lang.ast.scope.DiffableScope;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import gyro.parser.antlr4.BeamParser.ReferenceBodyContext;

import java.lang.reflect.InvocationTargetException;
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

    private List<ResourceQueryGroup> parseQueries(Scope scope) throws Exception {
        List<ResourceQueryGroup> groups = new ArrayList<>();
        for (Query query : queries) {
            List<ResourceQueryGroup> groupsForOneQuery = query.evaluate(scope, this);
            if (groups.isEmpty()) {
                groups = groupsForOneQuery;
            } else {
                List<ResourceQueryGroup> result = new ArrayList<>();
                for (ResourceQueryGroup left : groups) {
                    for (ResourceQueryGroup right : groupsForOneQuery) {
                        result.add(left.join(right));
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
                ResourceQuery<Resource> resourceQuery = getResourceQuery(scope);
                if (resourceQuery == null) {
                    throw new BeamException("Resource type " + type + " does not support external queries.");
                }

                if (queries.isEmpty()) {
                    resources = resourceQuery.queryAll();

                } else {
                    List<ResourceQueryGroup> groups = parseQueries(scope);

                    for (ResourceQueryGroup group : groups) {
                        group.merge();
                        resources.addAll(group.query());
                    }
                }

                resources.stream().forEach(r -> System.out.println(r.toDisplayString()));

                if (attribute != null) {
                    return resources.stream().map(r -> r.get(attribute)).collect(Collectors.toList());
                } else {
                    return resources;
                }

            } else if (name.endsWith("*")) {
                RootScope rootScope = scope.getRootScope();
                if (name.startsWith("STATE/")) {
                    rootScope = rootScope.getCurrent();
                    name = name.replaceFirst("STATE/", "");
                }

                Stream<Resource> s = rootScope.findAllResources().stream().filter(r -> type.equals(r.resourceType()));

                if (!name.equals("*")) {
                    String prefix = name.substring(0, name.length() - 1);
                    s = s.filter(r -> r.resourceIdentifier().startsWith(prefix));
                }

                List<ResourceQueryGroup> groups = parseQueries(scope);
                List<Resource> baseResources = s.collect(Collectors.toList());

                if (queries.isEmpty()) {
                    resources.addAll(baseResources);
                } else {
                    for (ResourceQueryGroup group : groups) {
                        resources.addAll(group.query(baseResources));
                    }
                }

                resources.stream().forEach(r -> System.out.println(r.toDisplayString()));

                if (attribute != null) {
                    return resources.stream().map(r -> r.get(attribute)).collect(Collectors.toList());
                } else {
                    return resources;
                }

            } else if (name.startsWith("STATE/")) {
                name = name.replaceFirst("STATE/", "");
                if (name.isEmpty()) {
                    throw new IllegalArgumentException();
                }

                String fullName = type + "::" + name;
                Resource resource = scope.getRootScope().getCurrent().findResource(fullName);

                if (attribute != null) {
                    return resource.get(attribute);

                } else {
                    return resource;
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

    public ResourceQuery<Resource> getResourceQuery(Scope scope) throws Exception {

        @SuppressWarnings("unchecked")
        Class<? extends ResourceQuery> resourceQueryClass = (Class<? extends ResourceQuery>) scope.getRootScope().getResourceQueryClasses().get(type);

        if (resourceQueryClass == null) {
            return null;
        }

        try {
            ResourceQuery<Resource> resourceQuery = resourceQueryClass.getConstructor().newInstance();
            resourceQuery.scope(new DiffableScope(scope));
            return resourceQuery;

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
    }
}
