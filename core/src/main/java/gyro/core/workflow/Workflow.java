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

import com.google.common.base.Preconditions;
import gyro.core.Abort;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.diff.Diff;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.util.ImmutableCollectors;

public class Workflow {

    private final String type;
    private final String name;
    private final RootScope root;
    private final List<Stage> stages;

    public Workflow(String type, String name, Scope scope) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.root = Preconditions.checkNotNull(scope).getRootScope();

        @SuppressWarnings("unchecked")
        List<Scope> stageScopes = (List<Scope>) scope.get("stage");

        if (stageScopes.isEmpty()) {
            throw new GyroException("Workflow requires 1 or more stages!");
        }

        this.stages = stageScopes.stream()
            .map(s -> new Stage(scope.getName(s), s))
            .collect(ImmutableCollectors.toList());
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<Stage> getStages() {
        return stages;
    }

    private RootScope copyCurrentRootScope() {
        RootScope current = root.getCurrent();
        RootScope scope = new RootScope(current.getFile(), current.getBackend(), null, current.getLoadFiles());
        scope.evaluate();
        return scope;
    }

    public void execute(
            GyroUI ui,
            State state,
            Resource currentResource,
            Resource pendingResource) {

        int stageIndex = 0;
        int stagesSize = stages.size();

        do {
            Stage stage = stages.get(stageIndex);
            String stageName = stage.getName();

            ui.write("\n@|magenta %d Executing %s stage|@\n", stageIndex + 1, stageName);

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            ui.indent();

            try {
                stageName = stage.execute(ui, state, currentResource, pendingResource, copyCurrentRootScope(), copyCurrentRootScope());

            } finally {
                ui.unindent();
            }

            if (stageName == null) {
                ++stageIndex;

            } else {
                stageIndex = -1;

                for (int i = 0; i < stagesSize; ++i) {
                    Stage s = stages.get(i);

                    if (s.getName().equals(stageName)) {
                        stageIndex = i;
                        break;
                    }
                }

                if (stageIndex < 0) {
                    throw new GyroException(String.format(
                        "Can't transition to @|bold %s|@ stage because it doesn't exist!",
                        stageName));
                }
            }

        } while (stageIndex < stagesSize);

        RootScope current = copyCurrentRootScope();
        RootScope pending = new RootScope(root.getFile(), root.getBackend(), current, root.getLoadFiles());

        pending.evaluate();
        pending.validate();

        Diff diff = new Diff(
            current.findResourcesIn(current.getLoadFiles()),
            pending.findResourcesIn(pending.getLoadFiles()));

        diff.diff();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nFinalize %s workflow?", name)) {
                ui.write("\n");
                diff.execute(ui, state);

            } else {
                throw new Abort();
            }
        }
    }

}
