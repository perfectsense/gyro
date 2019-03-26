package gyro.lang.ast.value;

import com.google.common.collect.ImmutableList;
import gyro.core.BeamException;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.lang.Credentials;
import gyro.lang.Resource;
import gyro.lang.ResourceFinder;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.query.AndQuery;
import gyro.lang.ast.query.ComparisonQuery;
import gyro.lang.ast.query.FieldValueQuery;
import gyro.lang.ast.query.FoundQuery;
import gyro.lang.ast.query.OrQuery;
import gyro.lang.ast.query.Query;
import gyro.lang.ast.scope.DiffableScope;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import gyro.parser.antlr4.BeamParser.ReferenceBodyContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        List<Query> queryList = context.queryExpression()
            .stream()
            .map(Query::create)
            .collect(Collectors.toList());

        String attributeValue = null;
        Integer size = queryList.size();
        if (size > 0) {
            Query last = queryList.get(size - 1);

            if (last instanceof FieldValueQuery) {
                attributeValue = ((FieldValueQuery) last).getValue();
                queryList.remove(size - 1);
            }
        }

        queries = ImmutableList.copyOf(queryList);
        this.attribute = attributeValue;
    }

    public ResourceReferenceNode(String type, Node nameNode, String attribute, Collection<Query> queries) {
        this.type = type;
        this.nameNode = nameNode;
        this.attribute = attribute;
        this.queries = ImmutableList.copyOf(queries);
    }

    private Query optimize(Query query, ResourceFinder<Resource> finder, Scope scope) throws Exception {
        if (query instanceof AndQuery) {
            Map<String, String> filters = new HashMap<>();
            List<Query> unsupported = new ArrayList<>();
            List<Query> newChildren = new ArrayList<>();

            for (Query child : ((AndQuery) query).getChildren()) {
                if (child instanceof ComparisonQuery) {
                    ComparisonQuery comparisonQuery = (ComparisonQuery) child;
                    if (comparisonQuery.isSupported(finder)) {
                        filters.putAll(comparisonQuery.getFilter(scope));
                    } else {
                        unsupported.add(child);
                    }

                } else {
                    unsupported.add(child);
                }
            }

            if (!filters.isEmpty()) {
                FoundQuery foundQuery = new FoundQuery(finder.find(findQueryCredentials(scope), filters));
                newChildren.add(foundQuery);
            }

            newChildren.addAll(unsupported);
            return new AndQuery(newChildren);

        } else if (query instanceof ComparisonQuery) {
            ComparisonQuery comparisonQuery = (ComparisonQuery) query;
            if (comparisonQuery.isSupported(finder)) {
                return new FoundQuery(finder.find(findQueryCredentials(scope), comparisonQuery.getFilter(scope)));

            } else {
                return query;
            }

        } else if (query instanceof OrQuery) {
            List<Query> newChildren = new ArrayList<>();
            for (Query child : ((OrQuery) query).getChildren()) {
                Query optimized = optimize(child, finder, scope);
                newChildren.add(optimized);
            }

            return new OrQuery(newChildren);

        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        if (nameNode != null) {
            String name = (String) nameNode.evaluate(scope);

            List<Resource> resources = new ArrayList<>();
            List<Query> queryList = new ArrayList<>(queries);
            if (name.startsWith("EXTERNAL/*")) {
                Class<? extends ResourceFinder> resourceQueryClass = scope.getRootScope().getResourceFinderClasses().get(type);

                if (resourceQueryClass == null) {
                    throw new BeamException("Resource type " + type + " does not support external queries.");
                }

                ResourceFinder<Resource> finder = resourceQueryClass.getConstructor().newInstance();

                queryList.clear();
                boolean isHead = true;
                for (Query q : queries) {
                    if (isHead) {
                        isHead = false;
                        Query optimized = optimize(q, finder, scope);
                        queryList.add(optimized);

                        resources = optimized.evaluate(type, scope, resources);
                        if (resources.isEmpty()) {
                            resources = finder.findAll(findQueryCredentials(scope));
                        }

                    } else {
                        queryList.add(q);
                    }
                }

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

                String resolveError = "Unable to resolve resource reference";
                if (resource == null) {
                    throw new DeferError(this, String.format("%s %s %s%nResource '%s %s' is not defined.%n",
                        resolveError, this, getLocation(), type, name));
                }

                DiffableScope diffableScope = scope.getClosest(DiffableScope.class);
                if (diffableScope != null) {
                    diffableScope.dependencies().add(resource);
                }

                if (attribute != null) {
                    if (DiffableType.getInstance(resource.getClass()).getFields().stream()
                            .map(DiffableField::getBeamName)
                            .anyMatch(attribute::equals)) {

                        return resource.get(attribute);

                    } else {
                        throw new BeamException(String.format("%s %s %s%nAttribute '%s' is not allowed in %s.%n",
                            resolveError, this, getLocation(), attribute, type));
                    }

                } else {
                    return resource;
                }
            }

            for (Query q : queryList) {
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

    private Credentials findQueryCredentials(Scope scope) {

        scope = scope.getRootScope();

        if (scope != null) {
            String name = (String) scope.get("resource-credentials");

            if (name == null) {
                name = "default";
            }

            for (Resource resource : scope.getRootScope().findAllResources()) {
                if (resource instanceof Credentials) {
                    Credentials credentials = (Credentials) resource;

                    if (credentials.resourceIdentifier().equals(name)) {
                        return credentials;
                    }
                }
            }
        }

        throw new IllegalStateException();
    }
}
