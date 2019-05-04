package gyro.core.scope;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.TypeDefinition;
import gyro.core.Credentials;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.core.plugin.PluginLoader;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceFinder;
import gyro.core.workflow.Workflow;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.DirectiveNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.PluginNode;
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
        throw new GyroException(String.format("[%s] directive isn't supported!", node.getDirective()));
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

        // Evaluate imports and plugins first.
        List<PairNode> keyValues = new ArrayList<>();
        List<PluginNode> plugins = new ArrayList<>();
        Map<String, VirtualResourceNode> virtualResourceNodes = rootScope.getVirtualResourceNodes();
        List<ResourceNode> workflowNodes = new ArrayList<>();
        List<Node> body = new ArrayList<>();

        for (Node item : node.getBody()) {
            if (item instanceof PairNode) {
                keyValues.add((PairNode) item);

            } else if (item instanceof PluginNode) {
                plugins.add((PluginNode) item);

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

        if (!plugins.isEmpty() && !(fileScope instanceof RootScope)) {
            throw new GyroException(String.format("Plugins are only allowed to be defined in '%s'.%nThe following plugins are found in '%s':%n%s",
                GyroCore.INIT_FILE,
                fileScope.getFile(),
                plugins.stream()
                    .map(Node::toString)
                    .collect(Collectors.joining("\n"))));
        }

        for (PairNode kv : keyValues) {
            visit(kv, fileScope);
        }

        for (PluginNode plugin : plugins) {
            Scope bodyScope = new Scope(scope);

            for (Node item : plugin.getBody()) {
                visit(item, bodyScope);
            }

            PluginLoader loader = new PluginLoader(bodyScope);

            loader.load();
            scope.getFileScope().getPluginLoaders().add(loader);
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
    public Object visitPlugin(PluginNode node, Scope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object visitResource(ResourceNode node, Scope scope) {
        String name = (String) visit(node.getName(), scope);
        String type = node.getType();
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
                        if (f.shouldBeDiffed()) {
                            continue;
                        }

                        String key = f.getGyroName();

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
                bodyScope.putIfAbsent(field.getGyroName(), field.getValue(anotherResource));
            }

        } else if (another instanceof Map) {
            ((Map<String, Object>) another).forEach(bodyScope::putIfAbsent);
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

        paramRootScope.findResources()
                .stream()
                .filter(Credentials.class::isInstance)
                .forEach(c -> vrScope.put(c.resourceType() + "::" + c.resourceIdentifier(), c));

        for (Node item : node.getBody()) {
            visit(item, resourceScope);
        }

        for (Resource resource : vrScope.findResources()) {
            if (!(resource instanceof Credentials)) {
                String newId = prefix + "." + resource.resourceIdentifier();

                resource.resourceIdentifier(newId);
                paramFileScope.put(resource.resourceType() + "::" + newId, resource);
            }
        }
    }

    @Override
    public Object visitVirtualResource(VirtualResourceNode node, Scope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitAndCondition(AndConditionNode node, Scope scope) {
        Boolean leftValue = toBoolean(visit(node.getLeft(), scope));
        Boolean rightValue = toBoolean(visit(node.getRight(), scope));

        return leftValue && rightValue;
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;

        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        return false;
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
        Boolean leftValue = toBoolean(visit(node.getLeft(), scope));
        Boolean rightValue = toBoolean(visit(node.getRight(), scope));

        return leftValue || rightValue;
    }

    @Override
    public Object visitValueCondition(ValueConditionNode node, Scope scope) {
        return visit(node.getLeft(), scope);
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
            Node expression = conditions.get(i);
            Boolean value = toBoolean(visit(expression, scope));

            if (value) {
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
            List<Query> queries = new ArrayList<>(node.getQueries());

            if (name.startsWith("EXTERNAL/*")) {
                Class<? extends ResourceFinder> resourceQueryClass = scope.getRootScope().getResourceFinderClasses().get(type);

                if (resourceQueryClass == null) {
                    throw new GyroException("Resource type " + type + " does not support external queries.");
                }

                ResourceFinder<Resource> finder = TypeDefinition.getInstance(resourceQueryClass).newInstance();
                boolean isHead = true;

                for (Query q : queries) {
                    if (isHead) {
                        isHead = false;
                        Query optimized = evaluator.optimize(q, finder, scope);
                        queries.add(optimized);

                        resources = evaluator.visit(optimized, new QueryContext(type, scope, resources));
                        if (resources.isEmpty()) {
                            resources = finder.findAll(evaluator.findQueryCredentials(scope));
                        }

                    } else {
                        queries.add(q);
                    }
                }

            } else if (name.endsWith("*")) {
                RootScope rootScope = scope.getRootScope();
                Stream<Resource> s = rootScope.findResources().stream().filter(r -> type.equals(r.resourceType()));

                if (!name.equals("*")) {
                    String prefix = name.substring(0, name.length() - 1);
                    s = s.filter(r -> r.resourceIdentifier().startsWith(prefix));
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
                            .map(DiffableField::getGyroName)
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

            for (Query q : queries) {
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
                    .filter(r -> type.equals(r.resourceType()));

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
