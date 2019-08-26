package gyro.core.resource;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("extends")
public class ExtendsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        processSource(scope, getArgument(scope, node, Object.class, 0));
    }

    @SuppressWarnings("unchecked")
    private void processSource(DiffableScope scope, Object source) {
        if (source instanceof Map) {
            ((Map<String, Object>) source).forEach((key, value) -> scope.computeIfAbsent(key, k -> clone(value)));

        } else if (source instanceof Resource) {
            processSource(scope, DiffableInternals.getScope((Resource) source));

        } else if (source instanceof String) {
            String name = (String) source;
            Resource resource = scope.getRootScope().findResource(name);

            if (resource == null) {
                throw new GyroException(String.format(
                    "Can't extend from @|bold %s|@ resource because it doesn't exist!",
                    name));
            }

            processSource(scope, resource);
        }
    }

    private Object clone(Object value) {
        if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;
            DiffableType<Diffable> type = DiffableType.getInstance(diffable);
            DiffableScope scope = diffable.scope;
            Diffable clone = type.newInstance(new DiffableScope(scope, null));

            DiffableInternals.setName(clone, diffable.name);
            type.setValues(clone, scope);
            return clone;

        } else if (value instanceof DiffableScope) {
            DiffableScope scope = (DiffableScope) value;
            DiffableScope clone = new DiffableScope(scope.getParent(), scope.getNode());

            clone.putAll(scope);
            return clone;

        } if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(this::clone)
                .collect(Collectors.toList());

        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).entrySet()
                .stream()
                .collect(
                    LinkedHashMap::new,
                    (m, e) -> m.put(clone(e.getKey()), clone(e.getValue())),
                    LinkedHashMap::putAll);

        } else if (value instanceof Set) {
            return ((Set<?>) value).stream()
                .map(this::clone)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        } else {
            return value;
        }
    }

}
