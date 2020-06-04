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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class DeleteAction extends Action {

    private final Node resource;

    public DeleteAction(Node resource) {
        this.resource = resource;
    }

    public Node getResource() {
        return resource;
    }

    @Override
    public void execute(
        GyroUI ui,
        State state,
        Scope scope,
        List<String> toBeRemoved,
        List<ReplaceResource> toBeReplaced,
        Workflow workflow) {

        RootScope pending = scope.getRootScope();
        RootScope current = pending.getCurrent();
        Set<String> modifiedIn = null;

        try {
            Resource currentResource = visitResource(this.resource, current);
            modifiedIn = DiffableInternals.getModifiedIn(currentResource);

            if (modifiedIn == null) {
                modifiedIn = new LinkedHashSet<>();
                modifiedIn.add(Workflow.MAIN_RESOURCE);
            }
            modifiedIn.add(workflow.getType());
            DiffableInternals.setModifiedIn(currentResource, modifiedIn);
            current.getWorkflowRemovedResources().putIfAbsent(currentResource.primaryKey(), currentResource);
        } catch (Defer e) {
            // This is to support recovery of delete action.
        }

        Resource pendingResource = visitResource(this.resource, pending);
        DiffableInternals.setModifiedIn(pendingResource, modifiedIn);
        toBeRemoved.add(pendingResource.primaryKey());
    }
}
