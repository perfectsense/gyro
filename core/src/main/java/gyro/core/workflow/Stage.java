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

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.Abort;
import gyro.core.GyroUI;
import gyro.core.scope.Defer;
import gyro.core.diff.Diff;
import gyro.core.resource.DiffableInternals;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.resource.Resource;
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

        Defer.execute(actions, a -> a.execute(ui, state, pendingRootScope, scope));
    }

    public void execute(
        GyroUI ui,
        State state,
        Resource currentResource,
        Resource pendingResource,
        RootScope currentRootScope,
        RootScope pendingRootScope) {

        apply(ui, state, currentResource, pendingResource, pendingRootScope);

        Diff diff = new Diff(
            currentRootScope.findResourcesIn(currentRootScope.getLoadFiles()),
            pendingRootScope.findResourcesIn(pendingRootScope.getLoadFiles()));

        diff.diff();

        if (confirmDiff && diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nContinue with %s stage?", name)) {
                ui.write("\n");

            } else {
                throw new Abort();
            }
        }

        diff.execute(ui, state);
    }

    public Stage prompt(GyroUI ui, RootScope currentRootScope) {
        if (transitions.isEmpty()) {
            currentRootScope.delete(Workflow.EXECUTION_FILE);
            return null;
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

}
