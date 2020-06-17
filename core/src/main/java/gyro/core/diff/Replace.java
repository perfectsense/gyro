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
import gyro.core.WorkflowReplacer;
import gyro.core.replacer.WorkflowReplacerSettings;
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
    private final Set<DiffableField> changedFields;
    private final Workflow workflow;
    private final WorkflowReplacer workflowReplacer;

    public Replace(Diffable currentDiffable, Diffable pendingDiffable, Set<DiffableField> changedFields) {
        this.currentDiffable = currentDiffable;
        this.pendingDiffable = pendingDiffable;
        this.changedFields = changedFields;

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

            this.workflowReplacer = DiffableInternals.getScope(pendingResource)
                .getSettings(WorkflowReplacerSettings.class)
                .getWorkflowReplacer();

        } else {
            this.workflow = null;
            this.workflowReplacer = null;
        }
    }

    @Override
    public Diffable getDiffable() {
        return pendingDiffable;
    }

    private void writeFields(GyroUI ui) {
        if (!ui.isVerbose()) {
            return;
        }

        for (DiffableField field : DiffableType.getInstance(pendingDiffable.getClass()).getFields()) {
            if (!field.shouldBeDiffed()) {
                if (changedFields.contains(field)) {
                    writeDifference(ui, field, currentDiffable, pendingDiffable);
                }
            }
        }
    }

    @Override
    public void writePlan(GyroUI ui) {
        ui.write("@|cyan ⇅ Replace %s|@", getLabel(currentDiffable, false));
        ui.write(" (because of %s, ", changedFields.stream()
            .filter(f -> !f.isUpdatable())
            .map(DiffableField::getName)
            .collect(Collectors.joining(", ")));

        if (workflowReplacer != null) {
            ui.write("using %s", workflowReplacer.label());
        } else if (workflow != null) {
            ui.write("using %s", workflow.getName());

        } else {
            ui.write("skipping without a workflow");
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
    public ExecutionResult execute(GyroUI ui, State state, List<ChangeProcessor> processors) {
        if (workflow == null && workflowReplacer == null) {
            return ExecutionResult.SKIPPED;
        }

        if (ui.isVerbose()) {
            ui.write("\n");
        }

        if (workflowReplacer != null) {
            ui.write("\n@|magenta ~ Executing %s workflow replacer|@\n", workflowReplacer.label());
            workflowReplacer.execute(ui, state, (Resource) currentDiffable, (Resource) pendingDiffable);
        } else {
            ui.write("\n@|magenta ~ Executing %s workflow|@", workflow.getName());
            workflow.execute(ui, state, (Resource) currentDiffable, (Resource) pendingDiffable);
        }
        state.update(this);
        return ExecutionResult.OK;
    }

}
