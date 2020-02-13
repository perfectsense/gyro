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
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.GyroUI;
import gyro.core.diff.Retry;
import gyro.core.metadata.MetadataDirectiveProcessor;
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

    private final String type;
    private final String name;
    private final RootScope root;
    private final Map<String, Stage> stages;

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

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
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
        RootScope scope = new RootScope(current.getFile(), current.getBackend(), null, current.getLoadFiles());
        scope.evaluate();
        return scope;
    }

    @SuppressWarnings("unchecked")
    public void execute(
        GyroUI ui,
        State state,
        Resource currentResource,
        Resource pendingResource) {

        List<String> executedStages = new ArrayList<>();
        Map<String, Object> execution = getExecution(root.getCurrent());
        Stage stage;

        if (execution != null) {
            executedStages.addAll((List<String>) execution.get("executedStages"));

            stage = getStage(executedStages.get(executedStages.size() - 1));

            ui.write("\n@|magenta · Resuming from %s stage|@\n", stage.getName());
            ui.indent();

            try {
                stage = stage.prompt(ui, DiffableInternals.getScope(currentResource).getRootScope());

            } finally {
                ui.unindent();
            }

        } else {
            stage = stages.values().iterator().next();
        }

        while (stage != null) {
            String stageName = stage.getName();
            MetadataDirectiveProcessor.putMetadata("currentStage", stageName);

            ui.write("\n@|magenta · Executing %s stage|@\n", stageName);

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            RootScope currentRoot = copyCurrentRootScope();

            ui.indent();

            try {
                stage.execute(ui, state, currentResource, pendingResource, currentRoot, copyCurrentRootScope());
                executedStages.add(stageName);

                try (GyroOutputStream output = currentRoot.openOutput(Workflow.EXECUTION_FILE)) {
                    output.write(ObjectUtils.toJson(ImmutableMap.of(
                        "type", DiffableType.getInstance(currentResource).getName(),
                        "name", DiffableInternals.getName(currentResource),
                        "workflow", name,
                        "executedStages", executedStages
                    )).getBytes(StandardCharsets.UTF_8));
                }

                stage = stage.prompt(ui, currentRoot);

            } finally {
                ui.unindent();
            }
        }

        throw Retry.INSTANCE;
    }

}
