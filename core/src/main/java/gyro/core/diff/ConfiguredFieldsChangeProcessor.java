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

import java.util.Collection;
import java.util.HashMap;
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

    private static final String SCALAR_FIELD = "__scalar";

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
    public void beforeUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        saveConfiguredFields(pending);
    }

    @Override
    public void afterUpdate(GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
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
        DiffableType<Resource> type = DiffableType.getInstance(resource);

        /*
         * "field"  -> { "__scalar"      : ['field1', 'field2'] }
         * "field2" -> { "<primarykey1>" : ['field1', 'field2']
         *               "<primarykey2>" : ['field1', 'field2'] }
         */
        Map<String, Map<String, Set<String>>> storedConfiguredFields = new HashMap<>();

        for (DiffableField field : type.getFields()) {
            if (Diffable.class.isAssignableFrom(field.getItemClass())) {
                Map<String, Set<String>> configuredFields = new HashMap<>();
                Object value = field.getValue(resource);
                if (value == null) {
                    continue;
                }

                if (field.isCollection()) {
                    Collection<Diffable> values = (Collection<Diffable>) value;

                    for (Diffable diffable : values) {
                        configuredFields.put(diffable.primaryKey(), DiffableInternals.getConfiguredFields(diffable));
                    }
                } else {
                    Diffable diffable = (Diffable) value;

                    configuredFields.put(SCALAR_FIELD, DiffableInternals.getConfiguredFields(diffable));
                }

                storedConfiguredFields.put(field.getName(), configuredFields);
            }
        }

        DiffableInternals.getScope(resource)
            .getSettings(ConfiguredFieldsSettings.class)
            .setStoredConfiguredFields(storedConfiguredFields);
    }

    private void restoreConfiguredFields(Resource resource) {
        DiffableType<Resource> type = DiffableType.getInstance(resource);

        Map<String, Map<String, Set<String>>> storedConfiguredFields = DiffableInternals.getScope(resource)
            .getSettings(ConfiguredFieldsSettings.class)
            .getStoredConfiguredFields();

        for (String key : storedConfiguredFields.keySet()) {
            DiffableField field = type.getField(key);
            Object value = field.getValue(resource);
            if (value == null) {
                continue;
            }

            Map<String, Set<String>> configuredFields = storedConfiguredFields.get(key);
            if (field.isCollection()) {
                Collection<Diffable> values = (Collection<Diffable>) value;

                for (Diffable diffable : values) {
                    DiffableInternals.getConfiguredFields(diffable).addAll(configuredFields.get(key));
                }
            } else {
                Diffable diffable = (Diffable) value;

                DiffableInternals.getConfiguredFields(diffable).addAll(configuredFields.get(SCALAR_FIELD));
            }
        }
    }

}
