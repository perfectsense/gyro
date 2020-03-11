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

package gyro.core.workflow;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.Abort;
import gyro.core.GyroOutputStream;
import gyro.core.GyroUI;
import gyro.core.diff.Diff;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.util.ImmutableCollectors;

public class Stage {

    private final Workflow workflow;
    private final String name;
    private final boolean confirmDiff;
    private final String transitionPrompt;
    private final List<Action> actions;
    private final List<Transition> transitions;

    public Stage(Workflow workflow, String name, Scope scope) {
        this.workflow = workflow;
        this.name = Preconditions.checkNotNull(name, "Stage requires a name!");
        this.confirmDiff = Boolean.TRUE.equals(scope.get("confirm-diff"));
        this.transitionPrompt = (String) scope.get("transition-prompt");
        this.actions = ImmutableList.copyOf(scope.getSettings(WorkflowSettings.class).getActions());

        @SuppressWarnings("unchecked")
        List<Scope> transitionScopes = (List<Scope>) scope.get("transition");

        if (transitionScopes != null) {
            this.transitions = transitionScopes.stream()
                .map(s -> new Transition(scope.getName(s), s))
                .collect(ImmutableCollectors.toList());

        } else {
            this.transitions = ImmutableList.of();
        }
    }

    public String getName() {
        return name;
    }

    public void apply(
        GyroUI ui,
        State state,
        Resource currentResource,
        Resource pendingResource,
        RootScope pendingRootScope) {

        DiffableScope pendingScope = DiffableInternals.getScope(pendingResource);
        FileScope pendingFileScope = pendingScope.getFileScope();

        Scope scope = new Scope(pendingRootScope.getFileScopes()
            .stream()
            .filter(s -> s.getFile().equals(pendingFileScope.getFile()))
            .findFirst()
            .orElse(null));

        for (Map.Entry<String, Object> entry : pendingFileScope.entrySet()) {
            Object value = entry.getValue();

            if (!(value instanceof Resource)) {
                scope.put(entry.getKey(), value);
            }
        }

        scope.put("NAME", DiffableInternals.getName(pendingResource));
        scope.put("CURRENT", currentResource);
        scope.put("PENDING", pendingResource);

        Defer.execute(actions, a -> a.execute(ui, state, scope));
    }

    public void execute(
        GyroUI ui,
        State state,
        Resource currentResource,
        RootScope pendingRootScope) {
        RootScope newPendingRootScope = copyRootScope(pendingRootScope);
        RootScope newCurrentRootScope = newPendingRootScope.getCurrent();

        Diff diff = new Diff(
            newCurrentRootScope.findResourcesIn(newCurrentRootScope.getLoadFiles()),
            newPendingRootScope.findResourcesIn(newPendingRootScope.getLoadFiles()));

        diff.diff();

        if (confirmDiff && diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nContinue with %s stage?", name)) {
                ui.write("\n");

            } else {
                throw new Abort();
            }
        }

        try (GyroOutputStream output = newCurrentRootScope.openOutput(Workflow.EXECUTION_FILE)) {
            output.write(ObjectUtils.toJson(ImmutableMap.of(
                "type", DiffableType.getInstance(currentResource).getName(),
                "name", DiffableInternals.getName(currentResource),
                "workflow", workflow.getName(),
                "executedStages", workflow.getExecutedStages().stream().map(Stage::getName).collect(Collectors.toList())
            )).getBytes(StandardCharsets.UTF_8));
        }

        diff.execute(ui, state);
    }

    public Stage prompt(GyroUI ui, State state, RootScope currentRootScope) {
        int transitionsSize = transitions.size();

        if (transitionsSize == 0) {
            currentRootScope.delete(Workflow.EXECUTION_FILE);
            state.setRemoveWorkflowFlag(true);
            state.save();
            return null;

        } else if (transitionsSize == 1) {
            return workflow.getStage(transitions.get(0).getTo());
        }

        while (true) {
            for (Transition transition : transitions) {
                ui.write("\n%s) %s", transition.getName(), transition.getDescription());
            }

            String selected = ui.readText("\n%s ", transitionPrompt != null ? transitionPrompt : "Next stage?");

            Stage selectedStage = transitions.stream()
                .filter(t -> selected.equals(t.getName()))
                .map(t -> workflow.getStage(t.getTo()))
                .findFirst()
                .orElse(null);

            if (selectedStage != null) {
                return selectedStage;

            } else {
                ui.write("[%s] isn't valid! Try again.\n", selected);
            }
        }
    }

    private RootScope copyRootScope(RootScope root) {
        RootScope pending = root;
        RootScope scope = new RootScope(
            pending.getFile(),
            pending.getBackend(),
            copyCurrentRootScope(pending),
            pending.getLoadFiles(),
            true);
        scope.evaluate();

        updateWorkflowFileScopes(scope, pending.getFileScopes());

        return scope;
    }

    private RootScope copyCurrentRootScope(RootScope root) {
        RootScope current = root.getCurrent();
        RootScope scope = new RootScope(current.getFile(), current.getBackend(), null, current.getLoadFiles(), true);
        scope.evaluate();

        updateWorkflowFileScopes(scope, current.getFileScopes());

        return scope;
    }

    private void updateWorkflowFileScopes(RootScope root, List<FileScope> fileScopes) {
        List<FileScope> workflowFileScopes = new ArrayList<>();

        for (FileScope fileScope : fileScopes) {
            FileScope workflowFileScope = new FileScope(fileScope.getRootScope(), fileScope.getFile());

            for (Map.Entry<String, Object> entry : fileScope.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Resource) {
                    if (DiffableInternals.isInWorkflow((Resource) value)) {
                        workflowFileScope.put(entry.getKey(), value);
                    }
                }
            }
            workflowFileScopes.add(workflowFileScope);
        }
        root.getFileScopes().clear();
        root.getFileScopes().addAll(workflowFileScopes);
    }
}
