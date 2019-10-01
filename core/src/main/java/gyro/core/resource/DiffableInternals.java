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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import gyro.core.diff.Change;
import gyro.core.diff.Replace;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.BlockNode;

public final class DiffableInternals {

    private DiffableInternals() {
    }

    public static boolean isExternal(Diffable diffable) {
        return diffable.external;
    }

    public static String getName(Diffable diffable) {
        return diffable.name;
    }

    public static void setName(Diffable diffable, String name) {
        diffable.name = name;
    }

    public static DiffableScope getScope(Diffable diffable) {
        return diffable.scope;
    }

    public static Set<String> getConfiguredFields(Diffable diffable) {
        if (diffable.configuredFields == null) {
            diffable.configuredFields = new LinkedHashSet<>();
        }

        return diffable.configuredFields;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Diffable> List<Modification<T>> getModifications(T diffable) {
        return (List) diffable.modifications;
    }

    public static Change getChange(Diffable diffable) {
        return diffable.change;
    }

    public static void setChange(Diffable diffable, Change change) {
        diffable.change = change;
    }

    public static void reevaluate(Diffable diffable) {
        DiffableScope oldScope = diffable.scope;

        if (oldScope != null) {
            BlockNode block = oldScope.getBlock();

            if (block != null) {
                DiffableScope newScope = new DiffableScope(oldScope);
                diffable.scope = newScope;

                newScope.getRootScope().getEvaluator().evaluateDiffable(block, newScope);
                DiffableType.getInstance(diffable).setValues(diffable, newScope);
                newScope.process(diffable);
            }
        }
    }

    public static void update(Diffable diffable, boolean newScope) {
        if (newScope) {
            diffable.scope = new DiffableScope(diffable.scope.getParent(), null);
        }

        updateChildren(diffable, newScope);
    }

    private static void updateChildren(Diffable diffable, boolean newScope) {
        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (field.shouldBeDiffed()) {
                String fieldName = field.getName();
                Object value = field.getValue(diffable);

                (value instanceof Collection ? ((Collection<?>) value).stream() : Stream.of(value))
                    .filter(Diffable.class::isInstance)
                    .map(Diffable.class::cast)
                    .forEach(d -> {
                        if (newScope) {
                            d.scope = new DiffableScope(diffable.scope, null);
                        }

                        d.parent = diffable;
                        d.name = fieldName;

                        updateChildren(d, newScope);
                    });
            }
        }
    }

    public static Object getStateValue(Diffable diffable, DiffableField field) {
        if (DiffableInternals.getChange(diffable) instanceof Replace) {
            return ((Replace) DiffableInternals.getChange(diffable)).getUpdatedValue(field);

        } else {
            return field.getValue(diffable);
        }
    }

}
