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

import java.util.Optional;
import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.auth.Credentials;
import gyro.core.scope.State;

public abstract class Resource extends Diffable {

    Boolean inWorkflow;

    public boolean isInWorkflow() {
        return Boolean.TRUE.equals(inWorkflow);
    }

    public void setInWorkflow(boolean inWorkflow) {
        this.inWorkflow = inWorkflow ? Boolean.TRUE : null;
    }

    public abstract boolean refresh();

    public abstract void create(GyroUI ui, State state) throws Exception;

    public abstract void update(GyroUI ui, State state, Resource current, Set<String> changedFieldNames)
        throws Exception;

    public abstract void delete(GyroUI ui, State state) throws Exception;

    public void testCreate(GyroUI ui, State state) throws Exception {
        DiffableType.getInstance(getClass()).getFields().forEach(f -> f.testUpdate(this));
    }

    public <C extends Credentials> C credentials(Class<C> credentialsClass) {
        return Credentials.getInstance(credentialsClass, getClass(), scope);
    }

    public Object get(String key) {
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getField(key))
            .map(f -> f.getValue(this))
            .orElse(null);
    }

    @Override
    public String primaryKey() {
        return String.format("%s::%s", DiffableType.getInstance(getClass()).getName(), name);
    }
}
