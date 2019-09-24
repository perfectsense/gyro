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

import gyro.core.GyroUI;
import gyro.core.scope.State;

import java.util.Set;

public abstract class Modification<T extends Diffable> extends Diffable {

    public void refresh(T current) {
    }

    public void beforeCreate(GyroUI ui, State state, T pending) throws Exception {
    }

    public void afterCreate(GyroUI ui, State state, T pending) throws Exception {
    }

    public void beforeUpdate(GyroUI ui, State state, T current, T pending, Set<DiffableField> changedFields) throws Exception {
    }

    public void afterUpdate(GyroUI ui, State state, T current, T pending, Set<DiffableField> changedFields) throws Exception {
    }

    public void beforeDelete(GyroUI ui, State state, T current) throws Exception {
    }

    public void afterDelete(GyroUI ui, State state, T current) throws Exception {
    }

}
