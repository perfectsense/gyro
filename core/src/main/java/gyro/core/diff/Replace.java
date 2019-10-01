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
import java.util.stream.Collectors;

import gyro.core.GyroUI;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.State;
import gyro.core.workflow.Workflow;
import gyro.core.workflow.WorkflowSettings;

public class Replace extends Change {

    private final Diffable currentDiffable;
    private final Diffable pendingDiffable;
    private final Set<DiffableField> replaceFields;
    private final Set<DiffableField> updateFields;
    private final Workflow workflow;

    public Replace(Diffable currentDiffable, Diffable pendingDiffable, Set<DiffableField> changedFields) {
        this.currentDiffable = currentDiffable;
        this.pendingDiffable = pendingDiffable;
        this.replaceFields = changedFields
            .stream()
            .filter(f -> !f.isUpdatable())
            .collect(Collectors.toSet());

        this.updateFields = changedFields
            .stream()
            .filter(DiffableField::isUpdatable)
            .collect(Collectors.toSet());

        if (pendingDiffable instanceof Resource) {
            Resource pendingResource = (Resource) pendingDiffable;

            this.workflow = DiffableInternals.getScope(pendingResource)
                .getRootScope()
                .getSettings(WorkflowSettings.class)
                .getWorkflows()
                .stream()
                .filter(w -> w.getType().equals(DiffableType.getInstance(pendingResource.getClass()).getName()))
                .findFirst()
                .orElse(null);

        } else {
            this.workflow = null;
        }
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    public Object getUpdatedValue(DiffableField field) {
        Diffable diffable = updateFields.contains(field) ? pendingDiffable : currentDiffable;
        return field.getValue(diffable);
    }

    private void writeFields(GyroUI ui) {
        if (!ui.isVerbose()) {
            return;
        }

        for (DiffableField field : DiffableType.getInstance(pendingDiffable.getClass()).getFields()) {
            if (!field.shouldBeDiffed()) {
                if (updateFields.contains(field)
                    || replaceFields.contains(field)) {
                    writeDifference(ui, field, currentDiffable, pendingDiffable);
                }
            }
        }
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|cyan ⇅ Replace %s|@", getLabel(currentDiffable, false));
        ui.write("（");

        if (!updateFields.isEmpty()) {
            ui.write("update %s, ", updateFields.stream()
                .map(DiffableField::getName)
                .collect(Collectors.joining(", ")));
        }

        ui.write("replace because of %s, ", replaceFields.stream()
            .map(DiffableField::getName)
            .collect(Collectors.joining(", ")));

        if (workflow != null) {
            ui.write("using %s", workflow.getName());

        } else {
            ui.write("skipping replace without a workflow");
        }

        ui.write(")");
        writeFields(ui);
    }

    @Override
    public void writeExecution(GyroUI ui) {
        ui.write("@|magenta ⇅ Replacing %s|@", getLabel(currentDiffable, true));
        writeFields(ui);
    }

    @Override
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) throws Exception {
        if (!updateFields.isEmpty()) {
            Update update = new Update(currentDiffable, pendingDiffable, updateFields);
            update.execute(ui, state, processors);
        }

        if (workflow == null) {
            if (!updateFields.isEmpty()) {
                return ExecutionResult.UPDATED;

            } else {
                return ExecutionResult.SKIPPED;
            }
        }

        if (ui.isVerbose()) {
            ui.write("\n");
        }

        ui.write("\n@|magenta ~ Executing %s workflow|@", workflow.getName());
        workflow.execute(ui, state, (Resource) currentDiffable, (Resource) pendingDiffable);
        state.update(this);
        return ExecutionResult.OK;
    }

}
