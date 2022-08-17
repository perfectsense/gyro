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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import gyro.core.GyroUI;
import gyro.core.auth.Credentials;
import gyro.core.scope.State;

public abstract class Resource extends Diffable {

    public abstract boolean refresh();

    public Map<? extends Resource, Boolean> batchRefresh(List<? extends Resource> resources) {
        Map<Resource, Boolean> refreshResults = new HashMap<>();

        ExecutorService refreshService = Executors.newWorkStealingPool(24);
        List<Refresh> refreshes = new ArrayList<>();

        for (Resource resource : resources) {
            refreshes.add(new Refresh(resource, refreshService.submit(() -> resource.refresh())));
        }

        try {
            for (Refresh refresh : refreshes) {
                refreshResults.put(refresh.resource, refresh.future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            refreshService.shutdown();
            throw new RuntimeException(e);
        }

        return refreshResults;
    }

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

    private static class Refresh {

        public final Resource resource;
        public final Future<Boolean> future;

        public Refresh(Resource resource, Future<Boolean> future) {
            this.resource = resource;
            this.future = future;
        }
    }
}
