package gyro.core.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.directive.DirectiveSettings;
import gyro.core.finder.Finder;
import gyro.core.finder.FinderSettings;
import gyro.core.finder.FinderType;
import gyro.core.finder.QueryContext;
import gyro.core.finder.QueryEvaluator;
import gyro.core.workflow.Workflow;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.DirectiveNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.lang.ast.block.VirtualResourceParameter;
import gyro.lang.ast.condition.AndConditionNode;
import gyro.lang.ast.condition.ComparisonConditionNode;
import gyro.lang.ast.condition.OrConditionNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.control.IfNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.lang.ast.value.ValueReferenceNode;
import gyro.lang.query.Query;
import gyro.util.CascadingMap;

public class NodeEvaluator implements NodeVisitor<Scope, Object> {

    public void visitBody(List<Node> body, Scope scope) {
        int bodySize = body.size();

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            List<Node> deferred = new ArrayList<>();

            for (Node node : body) {
                try {
                    visit(node, scope);

                } catch (DeferError error) {
                    errors.add(error);
                    deferred.add(node);
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (bodySize == deferred.size()) {
                throw errors.get(0);

            } else {
                body = deferred;
                bodySize = body.size();
            }
        }
    }

    @Override
    public Object visitDirective(DirectiveNode node, Scope scope) {
        String name = node.getName();

        DirectiveProcessor processor = scope.getRootScope()
            .getSettings(DirectiveSettings.class)
            .getProcessors()
            .get(name);

        if (processor == null) {
            throw new GyroException(String.format(
                "Can't find a processor for @%s directive!",
                name));
        }

        try {
            processor.process(
                scope,
                node.getArguments()
                    .stream()
                    .map(a -> visit(a, scope))
                    .collect(Collectors.toList()));

        } catch (Exception error) {
            throw new GyroException(String.format(
                "Can't process @%s directive! %s: %s",
                name,
                error.getClass().getName(),
                error.getMessage()));
        }

        return null;
    }

    @Override
    public Object visitPair(PairNode node, Scope scope) {
        String key = node.getKey();
        Node value = node.getValue();

        scope.put(key, visit(value, scope));
        scope.addValueNode(key, value);
        scope.getKeyNodes().put(key, node);

        return scope.get(key);
    }

    @Override
    public Object visitFile(FileNode node, Scope scope) {
        RootScope rootScope = scope.getRootScope();
        FileScope fileScope;

        if (rootScope.getFile().equals(node.getFile())) {
            fileScope = rootScope;

        } else {
            fileScope = new FileScope(rootScope, node.getFile());
            rootScope.getFileScopes().add(fileScope);
        }

        List<PairNode> keyValues = new ArrayList<>();
        Map<String, VirtualResourceNode> virtualResourceNodes = rootScope.getVirtualResourceNodes();
        List<ResourceNode> workflowNodes = new ArrayList<>();
        List<Node> body = new ArrayList<>();

        for (Node item : node.getBody()) {
            if (item instanceof PairNode) {
                keyValues.add((PairNode) item);

            } else if (item instanceof VirtualResourceNode) {
                VirtualResourceNode vrNode = (VirtualResourceNode) item;
                virtualResourceNodes.put(vrNode.getName(), vrNode);

            } else {
                if (item instanceof ResourceNode) {
                    ResourceNode rnNode = (ResourceNode) item;

                    if (rnNode.getType().equals("workflow")) {
                        workflowNodes.add(rnNode);
                        continue;
                    }
                }

                body.add(item);
            }
        }

        for (PairNode kv : keyValues) {
            visit(kv, fileScope);
        }

        List<Workflow> workflows = rootScope.getWorkflows();

        for (ResourceNode rn : workflowNodes) {
            workflows.add(new Workflow(rootScope, rn));
        }

        try {
            visitBody(body, fileScope);

        } catch (DeferError e) {
            rootScope.getFileScopes().remove(fileScope);
            throw e;
        }

        return null;
    }

    @Override
    public Object visitKeyBlock(KeyBlockNode node, Scope scope) {
        DiffableScope bodyScope = new DiffableScope(scope);

        for (Node item : node.getBody()) {
            visit(item, bodyScope);
        }

        String key = node.getKey();

        scope.addValue(key, bodyScope);
        scope.getKeyNodes().put(key, node);

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object visitResource(ResourceNode node, Scope scope) {
        String name = (String) visit(node.getName(), scope);
        String type = node.getType();
        String fullName = type + "::" + name;
        RootScope rootScope = scope.getRootScope();
        DiffableScope bodyScope = new DiffableScope(scope);

        // Initialize the bodyScope with the resource values from the current
        // state scope.
        Optional.ofNullable(rootScope.getCurrent())
                .map(s -> s.findResource(fullName))
                .ifPresent(r -> {
                    Set<String> configuredFields = r.configuredFields != null
                        ? r.configuredFields
                        : ImmutableSet.of();

                    for (DiffableField f : DiffableType.getInstance(r.getClass()).getFields()) {

                        // Don't copy nested diffables since they're handled
                        // by the diff system.
                        if (f.shouldBeDiffed()) {
                            continue;
                        }

                        String key = f.getName();

                        // Skip over fields that were previously configured
                        // so that their removals can be detected by the
                        // diff system.
                        if (configuredFields.contains(key)) {
                            continue;
                        }

                        bodyScope.put(key, f.getValue(r));
                    }
                });

        for (Node item : node.getBody()) {
            visit(item, bodyScope);
        }

        Class<? extends Resource> resourceClass = (Class<? extends Resource>) rootScope.getResourceClasses().get(type);

        if (resourceClass == null) {
            VirtualResourceNode vrNode = scope.getRootScope().getVirtualResourceNodes().get(type);

            if (vrNode != null) {
                createResources(vrNode, name, bodyScope);
                return null;

            } else {
                throw new IllegalArgumentException(String.format(
                        "Can't create resource of [%s] type!",
                        type));
            }
        }

        Resource resource = DiffableType.getInstance(resourceClass).newDiffable(null, name, bodyScope);

        resource.initialize(new LinkedHashMap<>(bodyScope));
        scope.getFileScope().put(fullName, resource);

        return null;
    }

    private void createResources(VirtualResourceNode node, String prefix, Scope paramScope) {
        FileScope paramFileScope = paramScope.getFileScope();

        RootScope vrScope = new RootScope(
            GyroCore.INIT_FILE,
            paramScope.getRootScope().getBackend(),
            null,
            paramScope.getRootScope().getLoadFiles());

        FileScope resourceScope = new FileScope(vrScope, paramFileScope.getFile());

        for (VirtualResourceParameter param : node.getParameters()) {
            String paramName = param.getName();

            if (!paramScope.containsKey(paramName)) {
                throw new GyroLanguageException(String.format("Required parameter '%s' is missing.", paramName));

            } else {
                vrScope.put(paramName, paramScope.get(paramName));
            }
        }

        RootScope paramRootScope = paramScope.getRootScope();

        vrScope.getResourceClasses().putAll(paramRootScope.getResourceClasses());
        vrScope.getFileScopes().add(resourceScope);

        for (Node item : node.getBody()) {
            visit(item, resourceScope);
        }

        for (Resource resource : vrScope.findResources()) {
            resource.name = prefix + "." + resource.name;
            paramFileScope.put(resource.primaryKey(), resource);
        }
    }

    @Override
    public Object visitVirtualResource(VirtualResourceNode node, Scope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitAndCondition(AndConditionNode node, Scope scope) {
        return test(node.getLeft(), scope) && test(node.getRight(), scope);
    }

    private boolean test(Node node, Scope scope) {
        Object value = visit(node, scope);

        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value);

        } else if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();

        } else if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();

        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;

        } else if (value instanceof String) {
            return !((String) value).isEmpty();

        } else {
            return value != null;
        }
    }

    @Override
    public Object visitComparisonCondition(ComparisonConditionNode node, Scope scope) {
        Object leftValue = visit(node.getLeft(), scope);
        Object rightValue = visit(node.getRight(), scope);

        switch (node.getOperator()) {
            case "==" : return leftValue.equals(rightValue);
            case "!=" : return !leftValue.equals(rightValue);
            default   : return false;
        }
    }

    @Override
    public Object visitOrCondition(OrConditionNode node, Scope scope) {
        return test(node.getLeft(), scope) || test(node.getRight(), scope);
    }

    @Override
    public Object visitValueCondition(ValueConditionNode node, Scope scope) {
        return test(node.getValue(), scope);
    }

    @Override
    public Object visitFor(ForNode node, Scope scope) {
        List<String> variables = node.getVariables();
        List<Node> items = node.getItems();
        int variablesSize = variables.size();
        int itemsSize = items.size();

        for (int i = 0; i < itemsSize; i += variablesSize) {
            Map<String, Object> values = new LinkedHashMap<>();
            Scope bodyScope = new Scope(scope, new CascadingMap<>(scope, values));

            for (int j = 0; j < variablesSize; j++) {
                int k = i + j;

                values.put(
                    variables.get(j),
                    k < itemsSize
                        ? visit(items.get(k), scope)
                        : null);
            }

            visitBody(node.getBody(), bodyScope);
            scope.getKeyNodes().putAll(bodyScope.getKeyNodes());
        }

        return null;
    }

    @Override
    public Object visitIf(IfNode node, Scope scope) {
        List<Node> conditions = node.getConditions();
        List<List<Node>> bodies = node.getBodies();

        for (int i = 0; i < conditions.size(); i++) {
            if (test(conditions.get(i), scope)) {
                visitBody(bodies.get(i), scope);
                return null;
            }
        }

        if (bodies.size() > conditions.size()) {
            visitBody(bodies.get(bodies.size() - 1), scope);
        }

        return null;
    }

    @Override
    public Object visitInterpolatedString(InterpolatedStringNode node, Scope scope) {
        StringBuilder builder = new StringBuilder();

        for (Object item : node.getItems()) {
            if (item instanceof Node) {
                item = visit((Node) item, scope);
            }

            if (item != null) {
                builder.append(item);
            }
        }

        return builder.toString();
    }

    @Override
    public Object visitList(ListNode node, Scope scope) {
        List<Object> list = new ArrayList<>();

        for (Node item : node.getItems()) {
            list.add(visit(item, scope));
        }

        return list;
    }

    @Override
    public Object visitMap(MapNode node, Scope scope) {
        Scope bodyScope = new Scope(scope);
        Map<String, Object> map = new LinkedHashMap<>();

        for (PairNode entry : node.getEntries()) {
            map.put(entry.getKey(), visit(entry, bodyScope));
        }

        return map;
    }

    @Override
    public Object visitResourceReference(ResourceReferenceNode node, Scope scope) {
        String type = node.getType();
        Node nameNode = node.getName();
        String path = node.getPath();

        if (nameNode != null) {
            QueryEvaluator evaluator = new QueryEvaluator(this);
            String name = (String) visit(nameNode, scope);
            List<Resource> resources = new ArrayList<>();
            List<Query> queries = null;

            if (name.startsWith("EXTERNAL/*")) {
                Class<? extends Finder<Resource>> finderClass = scope.getRootScope()
                    .getSettings(FinderSettings.class)
                    .getFinderClasses()
                    .get(type);

                if (finderClass == null) {
                    throw new GyroException("Resource type " + type + " does not support external queries.");
                }

                Finder<Resource> finder = FinderType.getInstance(finderClass).newInstance(scope);
                List<Query> nodeQueries = node.getQueries();
                queries = new ArrayList<>();

                if (!nodeQueries.isEmpty()) {
                    Query optimized = evaluator.optimize(nodeQueries.get(0), finder, scope);
                    resources = evaluator.visit(optimized, new QueryContext(type, scope, resources));

                    queries.add(optimized);

                    if (resources.isEmpty()) {
                        resources = finder.findAll();
                    }

                } else {
                    nodeQueries.subList(1, nodeQueries.size()).forEach(queries::add);
                }

            } else if (name.endsWith("*")) {
                RootScope rootScope = scope.getRootScope();

                Stream<Resource> s = rootScope.findResources()
                    .stream()
                    .filter(r -> type.equals(DiffableType.getInstance(r.getClass()).getName()));

                if (!name.equals("*")) {
                    String prefix = name.substring(0, name.length() - 1);
                    s = s.filter(r -> r.name().startsWith(prefix));
                }

                resources = s.collect(Collectors.toList());

            } else {
                String fullName = type + "::" + name;
                Resource resource = scope.getRootScope().findResource(fullName);

                if (resource == null) {
                    throw new DeferError(node);
                }

                if (path != null) {
                    if (DiffableType.getInstance(resource.getClass()).getFields().stream()
                            .map(DiffableField::getName)
                            .anyMatch(path::equals)) {

                        return resource.get(path);

                    } else {
                        throw new GyroException(String.format("Unable to resolve resource reference %s %s%nAttribute '%s' is not allowed in %s.%n",
                            node, node.getLocation(), path, type));
                    }

                } else {
                    return resource;
                }
            }

            for (Query q : (queries != null ? queries : node.getQueries())) {
                resources = evaluator.visit(q, new QueryContext(type, scope, resources));
            }

            if (path != null) {
                return resources.stream().map(r -> r.get(path)).collect(Collectors.toList());
            } else {
                return resources;
            }

        } else {
            Stream<Resource> s = scope.getRootScope()
                    .findResources()
                    .stream()
                    .filter(r -> type.equals(DiffableType.getInstance(r.getClass()).getName()));

            if (path != null) {
                return s.map(r -> r.get(path))
                        .collect(Collectors.toList());

            } else {
                return s.collect(Collectors.toList());
            }
        }
    }

    @Override
    public Object visitValue(ValueNode node, Scope scope) {
        return node.getValue();
    }

    @Override
    public Object visitValueReference(ValueReferenceNode node, Scope scope) {
        try {
            return scope.find(node.getPath());

        } catch (ValueReferenceException vre) {
            throw new GyroException(String.format("Unable to resolve value reference %s %s%n'%s' is not defined.%n",
                node, node.getLocation(), vre.getKey()));
        }
    }

}
