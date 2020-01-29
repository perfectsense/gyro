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

package gyro.core.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("extends")
public class ExtendsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    @SuppressWarnings("unchecked")
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        validateOptionArguments(node, "exclude", 0, 1);
        validateOptionArguments(node, "merge", 0, 1);

        boolean merge = Optional.ofNullable(getOptionArgument(scope, node, "merge", Boolean.class, 0))
            .orElse(false);

        Object source = getArgument(scope, node, Object.class, 0);
        Map<String, Object> sourceMap;

        if (source == null) {
            throw new GyroException("Can't extend from a null!");

        } else if (source instanceof Map) {
            sourceMap = (Map<String, Object>) source;

        } else if (source instanceof Resource) {
            Resource resource = (Resource) source;
            sourceMap = DiffableInternals.getScope(resource);

            if (sourceMap == null) {
                sourceMap = DiffableType.getInstance(resource)
                    .getFields()
                    .stream()
                    .collect(
                        LinkedHashMap::new,
                        (m, f) -> m.put(f.getName(), f.getValue(resource)),
                        LinkedHashMap::putAll);
            }

        } else {
            throw new GyroException(String.format(
                "Can't extend from @|bold %s|@ because it's an instance of @|bold %s|@!",
                source,
                source.getClass().getName()));
        }

        Set<String> excludes = Optional.ofNullable(getOptionArgument(scope, node, "exclude", Set.class, 0))
            .orElse(Collections.emptySet());

        sourceMap = sourceMap.entrySet()
            .stream()
            .filter(e -> !excludes.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (merge) {
            sourceMap.forEach((key, value) -> scope.put(key, merge(scope.get(key), value)));
        } else {
            sourceMap.forEach(scope::putIfAbsent);
        }
    }

    private Object merge(Object oldValue, Object newValue) {
        if (oldValue instanceof Collection && newValue instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> clone = (Collection<Object>) clone(oldValue);

            ((Collection<?>) newValue).stream()
                .map(this::clone)
                .forEach(clone::add);

            return clone;

        } else if (oldValue instanceof Map && newValue instanceof Map) {
            Map<?, ?> oldMap = (Map<?, ?>) oldValue;
            Map<?, ?> newMap = (Map<?, ?>) newValue;

            return Stream.of(oldMap, newMap)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .distinct()
                .collect(
                    LinkedHashMap::new,
                    (map, key) -> map.put(clone(key), merge(clone(oldMap.get(key)), newMap.get(key))),
                    Map::putAll);

        } else if (oldValue != null) {
            return oldValue;

        } else {
            return clone(newValue);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T clone(T value) {
        if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;
            DiffableType<Diffable> type = DiffableType.getInstance(diffable);

            if (type.isRoot()) {
                return value;

            } else {
                return (T) type.newInternal(clone(diffable.scope), diffable.name);
            }

        } else if (value instanceof DiffableScope) {
            DiffableScope scope = (DiffableScope) value;
            DiffableScope clone = new DiffableScope(scope.getParent(), scope.getBlock());

            clone.putAll(scope);
            return (T) clone;

        }
        if (value instanceof List) {
            return (T) ((List<?>) value).stream()
                .map(this::clone)
                .collect(Collectors.toList());

        } else if (value instanceof Map) {
            return (T) ((Map<?, ?>) value).entrySet()
                .stream()
                .collect(
                    LinkedHashMap::new,
                    (m, e) -> m.put(clone(e.getKey()), clone(e.getValue())),
                    LinkedHashMap::putAll);

        } else if (value instanceof Set) {
            return (T) ((Set<?>) value).stream()
                .map(this::clone)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        } else {
            return value;
        }
    }

}
