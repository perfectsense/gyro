/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public class ConfiguredFieldsChangeProcessor extends ChangeProcessor {

    @Override
    public void beforeRefresh(GyroUI ui, Resource resource) throws Exception {
        saveConfiguredFields(resource);
    }

    @Override
    public void afterRefresh(GyroUI ui, Resource resource) throws Exception {
        restoreConfiguredFields(resource);
    }

    @Override
    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
        saveConfiguredFields(resource);
    }

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        restoreConfiguredFields(resource);
    }

    @Override
    public void beforeUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) throws Exception {
        saveConfiguredFields(pending);
    }

    @Override
    public void afterUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) throws Exception {
        restoreConfiguredFields(pending);
    }

    /**
     * Store configured fields for subresources.
     *
     * These fields will be restored in the after* operations.
     *
     * @param resource Resource to store off configured fields.
     */
    private void saveConfiguredFields(Resource resource) {
        DiffableInternals.getScope(resource)
            .getSettings(ConfiguredFieldsSettings.class)
            .setStoredConfiguredFields(extractConfiguredFields(resource));
    }

    private DiffableConfiguredFields extractConfiguredFields(Diffable diffable) {
        DiffableConfiguredFields diffableConfiguredFields = new DiffableConfiguredFields();
        diffableConfiguredFields.setPrimaryKey(diffable.primaryKey());
        diffableConfiguredFields.setConfiguredFields(DiffableInternals.getConfiguredFields(diffable));
        Map<String, List<DiffableConfiguredFields>> children = new HashMap<>();
        diffableConfiguredFields.setChildren(children);

        DiffableType<Diffable> type = DiffableType.getInstance(diffable);

        for (DiffableField field : type.getFields()) {
            if (Diffable.class.isAssignableFrom(field.getItemClass())) {
                Object fieldValue = field.getValue(diffable);

                if (fieldValue == null) {
                    continue;
                }
                List<DiffableConfiguredFields> configuredFieldsHierarchies = new ArrayList<>();
                children.put(field.getName(), configuredFieldsHierarchies);

                if (field.isCollection()) {
                    for (Diffable fieldDiffable : (Collection<Diffable>) fieldValue) {
                        configuredFieldsHierarchies.add(extractConfiguredFields(fieldDiffable));
                    }
                } else {
                    configuredFieldsHierarchies.add(extractConfiguredFields((Diffable) fieldValue));
                }
            }
        }
        return diffableConfiguredFields;
    }

    private void restoreConfiguredFields(Resource resource) {
        updateConfiguredFields(
            resource,
            DiffableInternals.getScope(resource)
                .getSettings(ConfiguredFieldsSettings.class)
                .getStoredConfiguredFields());
    }

    private void updateConfiguredFields(Diffable diffable, DiffableConfiguredFields diffableConfiguredFields) {
        DiffableInternals.getConfiguredFields(diffable).addAll(diffableConfiguredFields.getConfiguredFields());
        DiffableType<Diffable> type = DiffableType.getInstance(diffable);

        for (Map.Entry<String, List<DiffableConfiguredFields>> childEntry : diffableConfiguredFields.getChildren()
            .entrySet()) {
            List<DiffableConfiguredFields> configuredFieldsHierarchies = childEntry.getValue();

            if (configuredFieldsHierarchies.isEmpty()) {
                continue;
            }
            DiffableField field = type.getField(childEntry.getKey());
            Object fieldValue = field.getValue(diffable);

            if (fieldValue == null) {
                continue;
            }

            if (field.isCollection()) {
                for (Diffable fieldDiffable : (Collection<Diffable>) fieldValue) {
                    String primaryKey = fieldDiffable.primaryKey();

                    if (primaryKey == null) {
                        continue;
                    }
                    configuredFieldsHierarchies.stream()
                        .parallel()
                        .filter(e -> primaryKey.equals(e.getPrimaryKey()))
                        .findAny()
                        .ifPresent(e -> updateConfiguredFields(fieldDiffable, e));
                }
            } else {
                updateConfiguredFields((Diffable) fieldValue, configuredFieldsHierarchies.get(0));
            }
        }
    }
}
