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

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.diff.GlobalChangeProcessor;
import gyro.core.scope.State;

public class ModificationChangeProcessor extends GlobalChangeProcessor {

    @Override
    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.beforeCreate(ui, state, resource);
        }
    }

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.afterCreate(ui, state, resource);
        }
    }

    @Override
    public void beforeUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current)) {
            modification.beforeUpdate(ui, state, current, pending, changedFields);
        }
    }

    @Override
    public void afterUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(current)) {
            modification.afterUpdate(ui, state, current, pending, changedFields);
        }
    }

    @Override
    public void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.beforeDelete(ui, state, resource);
        }
    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
        for (Modification modification : DiffableInternals.getModifications(resource)) {
            modification.afterDelete(ui, state, resource);
        }
    }

}
