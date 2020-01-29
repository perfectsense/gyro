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

package gyro.core.diff;

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.resource.DiffableField;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public abstract class ChangeProcessor {

    public void beforeRefresh(GyroUI ui, Resource resource) throws Exception {
    }

    public void afterRefresh(GyroUI ui, Resource resource) throws Exception {
    }

    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
    }

    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
    }

    public void beforeUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) throws Exception {
    }

    public void afterUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) throws Exception {
    }

    public void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
    }

    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
    }

}
