/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.scope;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.TypeDefinition;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.directive.DirectiveSettings;
import gyro.core.finder.FilterContext;
import gyro.core.finder.FilterEvaluator;
import gyro.core.reference.ReferenceResolver;
import gyro.core.reference.ReferenceSettings;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceVisitor;
import gyro.lang.ast.block.BlockNode;
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
import gyro.util.ImmutableCollectors;
import org.apache.commons.lang3.math.NumberUtils;

public class NodeEvaluator implements NodeVisitor<Scope, Object, RuntimeException> {

    private Map<String, Set<Node>> typeNodes;
    private List<Node> body;

    private static final LoadingCache<Class<? extends DirectiveProcessor>, Class<? extends Scope>> DIRECTIVE_PROCESSOR_SCOPE_CLASSES = CacheBuilder.newBuilder()
        .build(new CacheLoader<Class<? extends DirectiveProcessor>, Class<? extends Scope>>() {

           @Override
           @SuppressWarnings("unchecked")
           public Class<? extends Scope> load(Class<? extends DirectiveProcessor> directiveProcessorClass) {
               return (Class<? extends Scope>) TypeDefinition.getInstance(directiveProcessorClass).getInferredGenericTypeArgumentClass(DirectiveProcessor.class, 0);
           }
       });

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
            throw new GyroException("Can't do arithmetic with a null!");
        }

        Number leftNumber = NumberUtils.createNumber(left.toString());

        if (leftNumber == null) {
            throw new GyroException(String.format(
                "Can't do arithmetic on @|bold %s|@, an instance of @|bold %s|@, because it's not a number!",
                left,
                left.getClass().getName()));
        }

        Number rightNumber = NumberUtils.createNumber(right.toString());

        if (rightNumber == null) {
            throw new GyroException(String.format(
                "Can't do arithmetic on @|bold %s|@, an instance of @|bold %s|@, because it's not a number!",
                right,
                right.getClass().getName()));
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

        } else if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());

        } else if (left instanceof Comparable && left.getClass().isInstance(right)) {
            return ((Comparable<Object>) left).compareTo(right);

        } else if (right instanceof Comparable && right.getClass().isInstance(left)) {
            return 0 - ((Comparable<Object>) right).compareTo(left);

        } else {
            throw new GyroException(String.format(
                "Can't compare @|bold %s|@, an instance of @|bold %s|@, against @|bold %s|@, an instance of @|bold %s|@!",
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

    public static Object getValue(Node node, Object object, String key) {
        if ("*".equals(key)) {
            return new GlobCollection(object);

        } else if (object instanceof Diffable) {
            Diffable diffable = (Diffable) object;
            DiffableType<Diffable> type = DiffableType.getInstance(diffable);
            DiffableField field = type.getField(key);

            if (field != null) {
                return field.getValue(diffable);
            }

        } else if (object instanceof GlobCollection) {
            return ((GlobCollection) object).stream()
                .map(i -> getValue(node, i, key))
                .flatMap(v -> v instanceof Collection
                    ? ((Collection<?>) v).stream()
                    : Stream.of(v))
                .collect(Collectors.toList());

        } else if (object instanceof List) {
            try {
                Number index = NumberUtils.createNumber(key);
                List<?> list = (List<?>) object;
                int size = list.size();
                int i = index.intValue();

                if (i < 0) {
                    i += size;
                }

                if (i < 0 || i >= size) {
                    throw new GyroException(node, String.format(
                        "@|bold %s|@ isn't a valid index to a list of @|bold %s|@ items!",
                        key,
                        size));
                }

                return list.get(i);

            } catch (NumberFormatException error) {
                // Ignore and try to look up getter/method.
            }

        } else if (object instanceof Map) {
            return ((Map<?, ?>) object).get(key);
        }

        Class<?> aClass = object.getClass();
        BeanInfo info = Reflections.getBeanInfo(aClass);
        String methodName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

        Method method = Stream.of(info.getPropertyDescriptors())
            .filter(p -> p.getName().equals(methodName))
            .map(PropertyDescriptor::getReadMethod)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseGet(() -> Stream.of(aClass.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> !m.getReturnType().equals(void.class))
                .findFirst()
                .orElseThrow(() -> {
                    if (object instanceof Diffable) {
                        return new GyroException(node, String.format(
                            "Can't find the @|bold %s|@ field or property in the @|bold %s|@ type!",
                            key,
                            DiffableType.getInstance((Diffable) object).getName()));

                    } else {
                        return new GyroException(node, String.format(
                            "Can't find the @|bold %s|@ property in the @|bold %s|@ class!",
                            key,
                            aClass.getName()));
                    }
                }));

        return Reflections.invoke(method, object);
    }

    public List<Node> getBody() {
        return body;
    }

    public void evaluate(RootScope root, List<Node> body) {
        this.typeNodes = new HashMap<>();
        this.body = body;

        body.stream()
            .filter(FileNode.class::isInstance)
            .map(FileNode.class::cast)
            .map(FileNode::getBody)
            .flatMap(List::stream)
            .forEach(item -> addTypeNode(item, item));

        evaluateBody(body, root);
    }

    public void addTypeNode(Node top, Node node) {
        if (node instanceof ResourceNode) {
            typeNodes.computeIfAbsent(((ResourceNode) node).getType(), k -> new HashSet<>()).add(top);
        }

        if (node instanceof BlockNode) {
            for (Node item : ((BlockNode) node).getBody()) {
                addTypeNode(top, item);
            }
        }
    }

    public void evaluateBody(List<Node> body, Scope scope) {
        Defer.execute(body, i -> visit(i, scope));
    }

    private void removeTypeNode(Node node) {
        if (typeNodes != null) {
            for (Iterator<Set<Node>> i = typeNodes.values().iterator(); i.hasNext(); ) {
                Set<Node> nodes = i.next();

                nodes.remove(node);

                if (nodes.isEmpty()) {
                    i.remove();
                }
            }
        }
    }

    @Override
    public Object visitDirective(DirectiveNode node, Scope scope) {
        String name = node.getName();

        @SuppressWarnings("unchecked")
        DirectiveProcessor<Scope> processor = (DirectiveProcessor<Scope>) scope.getRootScope()
            .getSettings(DirectiveSettings.class)
            .getProcessor(name);

        if (processor == null) {
            throw new GyroException(
                node,
                String.format("Can't find a processor for the @|bold @%s|@ directive!", name));
        }

        try {
            Class<? extends Scope> scopeClass = DIRECTIVE_PROCESSOR_SCOPE_CLASSES.getUnchecked(processor.getClass());

            if (!scopeClass.isInstance(scope)) {
                throw new GyroException(String.format(
                    "Can't use the @|bold @%s|@ directive inside @|bold %s|@!",
                    name,
                    scope.getClass().getName()));
            }

            processor.process(scope, node);

        } catch (Exception error) {
            throw new GyroException(
                node,
                String.format("Can't process the @|bold @%s|@ directive!", name),
                error);
        }

        removeTypeNode(node);
        return null;
    }

    @Override
    public Object visitPair(PairNode node, Scope scope) {
        String key = (String) visit(node.getKey(), scope);
        Node value = node.getValue();

        scope.put(key, visit(value, scope));
        scope.putLocation(key, node);

        removeTypeNode(node);
        return scope.get(key);
    }

    @Override
    public Object visitFile(FileNode node, Scope scope) {
        RootScope rootScope = scope.getRootScope();
        List<FileScope> fileScopes = rootScope.getFileScopes();
        String file = node.getFile();

        FileScope fileScope = fileScopes.stream()
            .filter(f -> f.getFile().equals(file))
            .findFirst()
            .orElse(null);

        if (fileScope == null) {
            fileScope = new FileScope(rootScope, file);
            fileScopes.add(fileScope);
        }

        evaluateBody(node.getBody(), fileScope);
        removeTypeNode(node);
        return null;
    }

    public void evaluateDiffable(BlockNode node, Scope scope) {
        for (Node item : node.getBody()) {
            if (!(item instanceof DirectiveNode)) {
                visit(item, scope);
            }
        }

        for (Node item : node.getBody()) {
            if (item instanceof DirectiveNode) {
                visit(item, scope);
            }
        }
    }

    @Override
    public Object visitKeyBlock(KeyBlockNode node, Scope scope) {
        DiffableScope bodyScope = new DiffableScope(scope, node);

        evaluateDiffable(node, bodyScope);

        String key = node.getKey();

        String name = (String) Optional.ofNullable(node.getName())
            .map(n -> visit(n, scope))
            .orElse(null);

        scope.addValue(key, name, bodyScope);
        scope.putLocation(key, node);

        removeTypeNode(node);
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object visitResource(ResourceNode node, Scope scope) {
        String type = node.getType();
        String name = (String) visit(node.getName(), scope);
        DiffableScope bodyScope = new DiffableScope(scope, node);

        try {
            evaluateDiffable(node, bodyScope);

        } catch (Defer error) {
            throw new CreateDefer(error, type, name);
        }

        RootScope root = scope.getRootScope();
        Object value = root.get(type);

        if (value == null) {
            throw new Defer(node, String.format(
                "Can't create a resource of @|bold %s|@ type!",
                type));

        } else if (value instanceof Class) {
            Class<?> c = (Class<?>) value;

            if (!Resource.class.isAssignableFrom(c)) {
                throw new GyroException(String.format(
                    "Can't create a resource of @|bold %s|@ type using the @|bold %s|@ class!",
                    type,
                    c.getName()));
            }

            FileScope file = scope.getFileScope();
            String fullName = type + "::" + name;

            if (file.containsKey(fullName)) {
                Node location = file.getLocation(fullName);

                if (!node.equals(location)) {
                    throw new GyroException(
                        node,
                        String.format("@|bold %s %s|@ has been defined already!", type, name),
                        new GyroException(location, "Defined previously:"));
                }
            }

            DiffableType<Resource> resourceType = DiffableType.getInstance((Class<Resource>) c);
            Resource resource = resourceType.newInternal(bodyScope, name);

            Optional.ofNullable(root.getCurrent())
                .map(s -> s.findResource(fullName))
                .ifPresent(r -> copy(r, resource));

            bodyScope.process(resource);
            file.put(fullName, resource);
            file.putLocation(fullName, node);

        } else if (value instanceof ResourceVisitor) {
            ((ResourceVisitor) value).visit(name, bodyScope);

        } else {
            throw new GyroException(String.format(
                "Can't create a resource of @|bold %s|@ type using @|bold %s|@, an instance of @|bold %s|@!",
                type,
                value,
                value.getClass().getName()));
        }

        removeTypeNode(node);
        return null;
    }

    private void copy(Diffable currentResource, Diffable pendingResource) {
        if (currentResource == null) {
            return;
        }

        Set<String> currentConfiguredFields = DiffableInternals.getConfiguredFields(currentResource);
        Set<String> pendingConfiguredFields = DiffableInternals.getConfiguredFields(pendingResource);

        for (DiffableField field : DiffableType.getInstance(currentResource).getFields()) {
            String fieldName = field.getName();

            // Current        Pending          Action
            // -------------- ---------------- ----------
            // Configured     Not configured   Don't copy
            // Configured     Configured       Don't copy
            // Not configured Not configured   Copy
            // Not configured Configured       Don't copy
            if (!currentConfiguredFields.contains(fieldName) && !pendingConfiguredFields.contains(fieldName)) {
                field.setValue(pendingResource, field.getValue(currentResource));

            } else if (field.shouldBeDiffed()) {
                Object pendingValue = field.getValue(pendingResource);

                if (pendingValue != null) {
                    Map<Optional<String>, Diffable> subs = Optional.ofNullable(field.getValue(currentResource))
                        .map(v -> stream(v).collect(ImmutableCollectors.toMap(d -> Optional.ofNullable(d.primaryKey()))))
                        .orElseGet(ImmutableMap::of);

                    stream(pendingValue).forEach(r -> copy(subs.get(Optional.ofNullable(r.primaryKey())), r));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Stream<Diffable> stream(Object value) {
        return (value instanceof Collection
            ? ((Collection<Diffable>) value).stream()
            : Stream.of((Diffable) value));
    }

    @Override
    public Object visitBinary(BinaryNode node, Scope scope) {
        String operator = node.getOperator();
        BiFunction<Object, Object, Object> function = BINARY_FUNCTIONS.get(operator);

        if (function == null) {
            throw new GyroException(String.format(
                "@|bold %s|@ is not a valid binary operator!",
                operator));
        }

        removeTypeNode(node);

        return function.apply(
            visit(node.getLeft(), scope),
            visit(node.getRight(), scope));
    }

    @Override
    public Object visitIndexed(IndexedNode node, Scope scope) {
        Object value = visit(node.getValue(), scope);

        if (value == null) {
            removeTypeNode(node);
            return null;
        }

        for (Node indexNode : node.getIndexes()) {
            Object index = visit(indexNode, scope);

            if (index == null) {
                removeTypeNode(node);
                return null;
            }

            value = getValue(node, value, index.toString());

            if (value == null) {
                removeTypeNode(node);
                return null;
            }
        }

        removeTypeNode(node);
        return value;
    }

    @Override
    public Object visitInterpolatedString(InterpolatedStringNode node, Scope scope) {
        removeTypeNode(node);

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

        removeTypeNode(node);
        return list;
    }

    @Override
    public Object visitMap(MapNode node, Scope scope) {
        Scope bodyScope = new Scope(scope);
        Map<String, Object> map = new LinkedHashMap<>();

        for (PairNode entry : node.getEntries()) {
            map.put((String) visit(entry.getKey(), bodyScope), visit(entry, bodyScope));
        }

        removeTypeNode(node);
        return map;
    }

    @Override
    public Object visitReference(ReferenceNode node, Scope scope) {
        List<Object> arguments = node.getArguments()
            .stream()
            .map(v -> visit(v, scope))
            .collect(Collectors.toList());

        if (arguments.isEmpty()) {
            removeTypeNode(node);
            return null;
        }

        Object value = arguments.remove(0);

        if (value == null) {
            removeTypeNode(node);
            return null;
        }

        if (node.getArguments().get(0) instanceof ValueNode) {
            RootScope root = scope.getRootScope();
            String referenceName = (String) value;
            ReferenceResolver resolver = root.getSettings(ReferenceSettings.class).getResolver(referenceName);

            if (resolver != null) {
                try {
                    removeTypeNode(node);
                    return resolveFilters(node, scope, resolver.resolve(scope, arguments));

                } catch (Exception error) {
                    throw new GyroException(
                        node,
                        String.format("Can't resolve @|bold %s|@ reference!", referenceName),
                        error);
                }

            } else if (referenceName.contains("::")) {
                String resourceName = (String) arguments.remove(0);

                if (!arguments.isEmpty()) {
                    throw new GyroException("Too many arguments trying to resolve a resource by name!");
                }

                if (resourceName.endsWith("*")) {
                    if (typeNodes != null && typeNodes.containsKey(referenceName)) {
                        throw new WildcardDefer(node, referenceName);
                    }

                    Stream<Resource> s = root.findResources()
                        .stream()
                        .filter(r -> referenceName.equals(DiffableType.getInstance(r.getClass()).getName()));

                    if (!resourceName.equals("*")) {
                        String prefix = resourceName.substring(0, resourceName.length() - 1);
                        s = s.filter(r -> DiffableInternals.getName(r).startsWith(prefix));
                    }

                    value = s.collect(Collectors.toList());

                } else {
                    Resource resource = root.findResource(referenceName + "::" + resourceName);

                    if (resource == null) {
                        throw new FindDefer(node, referenceName, resourceName);
                    }

                    value = resource;
                }

            } else {
                value = scope.find(node, referenceName);
            }

            if (value == null) {
                removeTypeNode(node);
                return null;
            }
        }

        removeTypeNode(node);
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
        removeTypeNode(node);
        return node.getValue();
    }

}
