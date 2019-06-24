package gyro.core.resource;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.directive.DirectiveSettings;
import gyro.core.finder.FilterContext;
import gyro.core.finder.FilterEvaluator;
import gyro.core.reference.ReferenceResolver;
import gyro.core.reference.ReferenceSettings;
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
import gyro.lang.ast.value.IndexedNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.lang.filter.Filter;
import gyro.util.CascadingMap;
import org.apache.commons.lang3.math.NumberUtils;

public class NodeEvaluator implements NodeVisitor<Scope, Object> {

    public static Object getValue(Object object, String key) {
        if ("*".equals(key)) {
            return new GlobCollection(object);

        } else if (object instanceof Diffable) {
            Diffable diffable = (Diffable) object;
            DiffableType type = DiffableType.getInstance(diffable.getClass());
            DiffableField field = type.getField(key);

            if (field == null) {
                throw new GyroException(String.format(
                    "Can't find [%s] field in [%s] type!",
                    key,
                    type.getName()));
            }

            return field.getValue(diffable);

        } else if (object instanceof GlobCollection) {
            return ((GlobCollection) object).stream()
                .map(i -> getValue(i, key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        } else if (object instanceof List) {
            Number index = NumberUtils.createNumber(key);

            if (index != null) {
                return ((List<?>) object).get(index.intValue());

            } else {
                return null;
            }

        } else if (object instanceof Map) {
            return ((Map<?, ?>) object).get(key);

        } else {
            Class<?> aClass = object.getClass();
            BeanInfo info;

            try {
                info = Introspector.getBeanInfo(aClass);

            } catch (IntrospectionException error) {
                throw new GyroException(String.format(
                    "Can't find any properties for [%s] class!",
                    aClass.getName()));
            }

            String methodName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

            Method getter = Stream.of(info.getPropertyDescriptors())
                .filter(p -> p.getName().equals(methodName))
                .map(PropertyDescriptor::getReadMethod)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new GyroException(String.format(
                    "Can't find [%s] property in [%s] class!",
                    key,
                    aClass.getName())));

            try {
                return getter.invoke(object);

            } catch (IllegalAccessException | InvocationTargetException error) {
                throw new GyroException(error.getMessage());
            }
        }
    }

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
        String key = (String) visit(node.getKey(), scope);
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

        resource.initialize(bodyScope.isExtended() ? new LinkedHashMap<>(bodyScope) : bodyScope);
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
        Object value = visit(node.getValue(), scope);

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            int variablesSize = variables.size();
            int listSize = list.size();

            for (int i = 0; i < listSize; i += variablesSize) {
                Map<String, Object> values = new LinkedHashMap<>();

                for (int j = 0; j < variablesSize; j++) {
                    int k = i + j;

                    values.put(
                        variables.get(j),
                        k < listSize
                            ? list.get(k)
                            : null);
                }

                visitForBody(node, scope, values);
            }

        } else if (value instanceof Map) {
            String keyVariable = variables.get(0);

            if (variables.size() > 1) {
                String valueVariable = variables.get(1);

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    values.put(valueVariable, entry.getValue());
                    visitForBody(node, scope, values);
                }

            } else {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    visitForBody(node, scope, values);
                }
            }

        } else {
            throw new GyroException("Can't iterate over a non-collection!");
        }

        return null;
    }

    private void visitForBody(ForNode node, Scope scope, Map<String, Object> values) {
        Scope bodyScope = new Scope(scope, new CascadingMap<>(scope, values));

        visitBody(node.getBody(), bodyScope);
        scope.getKeyNodes().putAll(bodyScope.getKeyNodes());
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
    public Object visitIndexedNode(IndexedNode node, Scope context) {
        Object value = visit(node.getValue(), context);

        if (value == null) {
            return null;
        }

        for (Node index : node.getIndexes()) {
            value = getValue(value, (String) visit(index, context));

            if (value == null) {
                return null;
            }
        }

        return value;
    }

    @Override
    public Object visitInterpolatedString(InterpolatedStringNode node, Scope scope) {
        return node.getItems()
            .stream()
            .map(i -> visit(i, scope))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining());
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
            map.put((String) visit(entry.getKey(), bodyScope), visit(entry, bodyScope));
        }

        return map;
    }

    @Override
    public Object visitReference(ReferenceNode node, Scope scope) {
        List<Object> arguments = node.getArguments()
            .stream()
            .map(v -> visit(v, scope))
            .collect(Collectors.toList());

        if (arguments.isEmpty()) {
            return null;
        }

        Object value = arguments.remove(0);

        if (value == null) {
            return null;
        }

        if (node.getArguments().get(0) instanceof ValueNode) {
            RootScope root = scope.getRootScope();
            String referenceName = (String) value;

            ReferenceResolver resolver = root.getSettings(ReferenceSettings.class)
                .getResolvers()
                .get(referenceName);

            if (resolver != null) {
                try {
                    return resolveFilters(node, scope, resolver.resolve(scope, arguments));

                } catch (Exception error) {
                    throw new GyroException(error.getMessage());
                }

            } else if (referenceName.contains("::")) {
                String resourceName = (String) arguments.remove(0);

                if (resourceName.endsWith("*")) {
                    Stream<Resource> s = root.findResources()
                        .stream()
                        .filter(r -> referenceName.equals(DiffableType.getInstance(r.getClass()).getName()));

                    if (!resourceName.equals("*")) {
                        String prefix = resourceName.substring(0, resourceName.length() - 1);
                        s = s.filter(r -> r.name().startsWith(prefix));
                    }

                    value = s.collect(Collectors.toList());

                } else {
                    Resource resource = root.findResource(referenceName + "::" + resourceName);

                    if (resource == null) {
                        throw new DeferError(node);
                    }

                    value = resource;
                }

            } else {
                value = scope.find(referenceName);
            }

            if (value == null) {
                return null;
            }
        }

        return resolveFilters(node, scope, ReferenceResolver.resolveRemaining(scope, arguments, value));
    }

    private Object resolveFilters(ReferenceNode node, Scope scope, Object value) {
        if (value == null) {
            return null;
        }

        List<Filter> filters = node.getFilters();

        if (filters == null || filters.isEmpty()) {
            return value;
        }

        FilterEvaluator evaluator = new FilterEvaluator();

        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                .filter(v -> filters.stream().allMatch(f -> evaluator.visit(f, new FilterContext(scope, v))))
                .collect(Collectors.toList());

        } else {
            for (Filter f : filters) {
                if (!evaluator.visit(f, new FilterContext(scope, value))) {
                    return null;
                }
            }

            return value;
        }
    }

    @Override
    public Object visitValue(ValueNode node, Scope scope) {
        return node.getValue();
    }

}
