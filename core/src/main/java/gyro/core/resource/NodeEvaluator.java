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
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.directive.DirectiveSettings;
import gyro.core.finder.FilterContext;
import gyro.core.finder.FilterEvaluator;
import gyro.core.reference.ReferenceResolver;
import gyro.core.reference.ReferenceSettings;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.BinaryNode;
import gyro.lang.ast.value.IndexedNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.lang.filter.Filter;
import org.apache.commons.lang3.math.NumberUtils;

public class NodeEvaluator implements NodeVisitor<Scope, Object> {

    private static final Map<String, BiFunction<Object, Object, Object>> BINARY_FUNCTIONS = ImmutableMap.<String, BiFunction<Object, Object, Object>>builder()
        .put("*", (l, r) -> doArithmetic(l, r, (ld, rd) -> ld * rd, (ll, rl) -> ll * rl))
        .put("/", (l, r) -> doArithmetic(l, r, (ld, rd) -> ld / rd, (ll, rl) -> ll / rl))
        .put("%", (l, r) -> doArithmetic(l, r, (ld, rd) -> ld % rd, (ll, rl) -> ll % rl))
        .put("+", (l, r) -> doArithmetic(l, r, Double::sum, Long::sum))
        .put("-", (l, r) -> doArithmetic(l, r, (ld, rd) -> ld - rd, (ll, rl) -> ll - rl))
        .put("=", Objects::equals)
        .put("!=", (l, r) -> !Objects.equals(l, r))
        .put("<", (l, r) -> compare(l, r) < 0)
        .put("<=", (l, r) -> compare(l, r) <= 0)
        .put(">", (l, r) -> compare(l, r) > 0)
        .put(">=", (l, r) -> compare(l, r) >= 0)
        .put("and", (l, r) -> test(l) && test(r))
        .put("or", (l, r) -> test(l) && test(r))
        .build();

    private static Object doArithmetic(Object left, Object right, DoubleBinaryOperator doubleOperator, LongBinaryOperator longOperator) {
        if (left == null || right == null) {
            throw new GyroException("Can't do arithmetic on a null!");
        }

        Number leftNumber = NumberUtils.createNumber(left.toString());

        if (leftNumber == null) {
            throw new GyroException(String.format(
                "Can't do arithmetic on [%s] because it's not a number!",
                left));
        }

        Number rightNumber = NumberUtils.createNumber(right.toString());

        if (rightNumber == null) {
            throw new GyroException(String.format(
                "Can't do arithmetic on [%s] because it's not a number!",
                right));
        }

        if (leftNumber instanceof Float
            || leftNumber instanceof Double
            || rightNumber instanceof Float
            || rightNumber instanceof Double) {

            return doubleOperator.applyAsDouble(leftNumber.doubleValue(), rightNumber.doubleValue());

        } else {
            return longOperator.applyAsLong(leftNumber.longValue(), rightNumber.longValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object left, Object right) {
        if (left == null || right == null) {
            throw new GyroException("Can't compare against a null!");

        } else if (left instanceof Comparable
            && left.getClass().isInstance(right)) {

            return ((Comparable<Object>) left).compareTo(right);

        } else {
            throw new GyroException(String.format(
                "Can't compare [%s], an instance of [%s], against [%s], an instance of [%s]!",
                left,
                left.getClass().getName(),
                right,
                right.getClass().getName()));
        }
    }

    public static boolean test(Object value) {
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
            processor.process(scope, node);

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
        List<Node> body = new ArrayList<>();

        for (Node item : node.getBody()) {
            if (item instanceof PairNode) {
                keyValues.add((PairNode) item);

            } else {
                body.add(item);
            }
        }

        for (PairNode kv : keyValues) {
            visit(kv, fileScope);
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

        String name = (String) Optional.ofNullable(node.getName())
            .map(n -> visit(n, scope))
            .orElse(null);

        scope.addValue(key, name, bodyScope);
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

        Object value = rootScope.get(type);

        if (value == null) {
            throw new DeferError(node);

        } else if (value instanceof Class) {
            Class<?> c = (Class<?>) value;

            if (Resource.class.isAssignableFrom(c)) {
                Resource resource = DiffableType.getInstance((Class<? extends Resource>) c).newDiffable(null, name, bodyScope);

                resource.initialize(bodyScope.isExtended() ? new LinkedHashMap<>(bodyScope) : bodyScope);
                scope.getFileScope().put(fullName, resource);

            } else {
                throw new GyroException(String.format(
                    "Can't create a resource of [%s] type using [%s] class!",
                    type,
                    c.getName()));
            }

        } else if (value instanceof ResourceVisitor) {
            ((ResourceVisitor) value).visit(name, bodyScope);

        } else {
            throw new GyroException(String.format(
                "Can't create a resource of [%s] type using [%s], an instance of [%s]!",
                type,
                value,
                value.getClass().getName()));
        }

        return null;
    }

    @Override
    public Object visitBinary(BinaryNode node, Scope scope) {
        String operator = node.getOperator();
        BiFunction<Object, Object, Object> function = BINARY_FUNCTIONS.get(operator);

        if (function == null) {
            throw new GyroException(String.format(
                "[%s] is not a valid binary operator!",
                operator));
        }

        return function.apply(
            visit(node.getLeft(), scope),
            visit(node.getRight(), scope));
    }

    @Override
    public Object visitIndexed(IndexedNode node, Scope context) {
        Object value = visit(node.getValue(), context);

        if (value == null) {
            return null;
        }

        for (Node indexNode : node.getIndexes()) {
            Object index = visit(indexNode, context);

            if (index == null) {
                return null;
            }

            value = getValue(value, index.toString());

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

                if (!arguments.isEmpty()) {
                    throw new GyroException("Too many arguments trying to resolve resource by name!");
                }

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

        return resolveFilters(node, scope, value);
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
