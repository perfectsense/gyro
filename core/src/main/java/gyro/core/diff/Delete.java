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

import java.util.List;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public class Delete extends Change {

    private final Diffable diffable;

    public Delete(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|red - Delete %s|@", getLabel(diffable, false));
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta - Deleting %s|@", getLabel(diffable, true));
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) throws Exception {
        Resource resource = (Resource) diffable;

        state.update(this);

        for (ChangeProcessor processor : processors) {
            processor.beforeDelete(ui, state, resource);
        }

        if (!state.isTest()) {
            resource.delete(ui, state);
        }

        for (ChangeProcessor processor : processors) {
            processor.afterDelete(ui, state, resource);
        }

        return ExecutionResult.OK;
    }

}
