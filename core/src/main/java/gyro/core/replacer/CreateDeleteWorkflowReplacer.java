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

package gyro.core.replacer;

import java.util.Arrays;
import java.util.List;

import gyro.core.GyroUI;
import gyro.core.Type;
import gyro.core.WorkflowReplacer;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

@Type("create-delete")
public class CreateDeleteWorkflowReplacer extends WorkflowReplacer {

    private Resource newResource;

    @Override
    public List<Stage> getStages(GyroUI ui, State state, Resource currentResource, Resource pendingResource) {
        Stage swap = new Stage(
            "swap",
            "Swap new with old resource, delete old.",
            true,
            () -> swap(ui, state, currentResource, pendingResource));
        Stage reset = new Stage(
            "reset",
            "Reset.",
            true,
            () -> reset(ui, state, currentResource, pendingResource));
        Stage create = new Stage(
            "create",
            "Create new resource.",
            true,
            () -> create(ui, state, currentResource, pendingResource),
            Arrays.asList(swap, reset));

        return Arrays.asList(create, swap, reset);
    }

    private void create(GyroUI ui, State state, Resource currentResource, Resource pendingResource) {
        newResource = extendResource(pendingResource, DiffableInternals.getName(pendingResource) + "-create-delete");
        queueCreate(newResource);
    }

    private void swap(GyroUI ui, State state, Resource currentResource, Resource pendingResource) {
        queueReplace(currentResource, newResource);
    }

    private void reset(GyroUI ui, State state, Resource currentResource, Resource pendingResource) {
        queueDelete(newResource);
    }

    @Override
    public String label() {
        return "create then delete";
    }
}
