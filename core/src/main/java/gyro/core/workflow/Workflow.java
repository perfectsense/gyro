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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.GyroUI;
import gyro.core.diff.Retry;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.util.ImmutableCollectors;

public class Workflow {

    public static final String EXECUTION_FILE = "workflow-execution.json";
    public static final String STAGE_TYPE_NAME = "stage";

    private static final List<Workflow> SUCCESSFULLY_EXECUTED_WORKFLOWS = new ArrayList<>();

    private final String type;
    private final String name;
    private final RootScope root;
    private final Map<String, Stage> stages;
    private final List<String> executedStages = new ArrayList<>();

    public Workflow(String type, String name, Scope scope) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.root = Preconditions.checkNotNull(scope).getRootScope();

        @SuppressWarnings("unchecked")
        List<Scope> stageScopes = (List<Scope>) scope.get(STAGE_TYPE_NAME);

        if (stageScopes.isEmpty()) {
            throw new GyroException("Workflow requires 1 or more stages!");
        }

        this.stages = stageScopes.stream()
            .map(s -> new Stage(this, scope.getName(s), s))
            .collect(ImmutableCollectors.toMap(Stage::getName));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getExecution(RootScope root) {
        try (GyroInputStream input = root.openInput(Workflow.EXECUTION_FILE)) {
            return (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(input, StandardCharsets.UTF_8));

        } catch (GyroException error) {
            return null;

        } catch (IOException error) {
            throw new GyroException(error);
        }
    }

    public static List<Workflow> getSuccessfullyExecutedWorkflows() {
        return SUCCESSFULLY_EXECUTED_WORKFLOWS;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<String> getExecutedStages() {
        return executedStages;
    }

    public Stage getStage(String name) {
        Stage stage = stages.get(name);

        if (stage == null) {
            throw new GyroException(String.format(
                "Can't find @|bold %s|@ stage in @|bold %s|@ workflow!",
                name,
                this.name));
        }

        return stage;
    }

    private RootScope copyCurrentRootScope() {
        RootScope current = root.getCurrent();
        RootScope scope = new RootScope(current.getFile(), current.getBackend(), null, current.getLoadFiles(), true);
        scope.evaluate();

        return scope;
    }

    private RootScope copyPendingRootScope() {
        RootScope pending = root;
        RootScope scope = new RootScope(
            pending.getFile(),
            pending.getBackend(),
            copyCurrentRootScope(),
            pending.getLoadFiles(),
            true);
        scope.evaluate();

        return scope;
    }

    @SuppressWarnings("unchecked")
    public void execute(
        GyroUI ui,
        State state,
        Resource currentResource,
        Resource pendingResource) {

        Map<String, Object> execution = getExecution(root.getCurrent());
        String stageName;

        if (execution != null) {
            executedStages.addAll((List<String>) execution.get("executedStages"));
            stageName = executedStages.get(executedStages.size() - 1);
            ui.write("\n@|magenta · Resuming from %s stage|@\n", stageName);
        } else {
            stageName = stages.values().iterator().next().getName();
        }

        // TODO: optimize performance.
        while (stageName != null) {
            ui.write("\n@|magenta · Executing %s stage|@\n", stageName);

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            RootScope pendingRoot = copyPendingRootScope();

            int indexOf = executedStages.indexOf(stageName);

            if (indexOf > -1) {
                ListIterator<String> iterator = executedStages.listIterator(indexOf + 1);

                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            } else {
                executedStages.add(stageName);
            }

            RootScope currentRoot = pendingRoot.getCurrent();

            try (GyroOutputStream output = currentRoot.openOutput(Workflow.EXECUTION_FILE)) {
                output.write(ObjectUtils.toJson(ImmutableMap.of(
                    "type", DiffableType.getInstance(currentResource).getName(),
                    "name", DiffableInternals.getName(currentResource),
                    "workflow", name,
                    "executedStages", executedStages
                )).getBytes(StandardCharsets.UTF_8));
            }

            Stage stage = null;

            for (String executedStage : executedStages) {
                stage = getStage(executedStage);
                stage.apply(ui, state, currentResource, pendingResource, pendingRoot);
            }

            ui.indent();

            try {
                stage.execute(ui, state, pendingRoot);
                stageName = Optional.ofNullable(stage.prompt(ui, state, currentRoot))
                    .map(Stage::getName)
                    .orElse(null);

            } finally {
                ui.unindent();
            }
        }
        SUCCESSFULLY_EXECUTED_WORKFLOWS.add(this);

        throw Retry.INSTANCE;
    }

}
