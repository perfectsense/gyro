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
import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.State;

public class Create extends Change {

    private final Diffable diffable;

    public Create(Diffable diffable) {
        this.diffable = diffable;
    }

    @Override
    public Diffable getDiffable() {
        return diffable;
    }

    private void writeFields(GyroUI ui) {
        Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            if (!field.shouldBeDiffed() && configuredFields.contains(field.getName())) {
                ui.write("\n· %s: %s", field.getName(), stringify(field.getValue(diffable)));
            }
        }
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|green + Create %s|@", getLabel(diffable, false));

        if (ui.isVerbose()) {
            writeFields(ui);
        }
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta + Creating %s|@", getLabel(diffable, true));
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) throws Exception {
        Resource resource = (Resource) diffable;

        state.update(this);

        for (ChangeProcessor processor : processors) {
            processor.beforeCreate(ui, state, resource);
        }

        if (state.isTest()) {
            resource.testCreate(ui, state);

        } else {
            resource.create(ui, state);
        }

        for (ChangeProcessor processor : processors) {
            processor.afterCreate(ui, state, resource);
        }

        return ExecutionResult.OK;
    }

}
