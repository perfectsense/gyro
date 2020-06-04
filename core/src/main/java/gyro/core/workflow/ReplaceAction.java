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

import com.google.common.base.Preconditions;
import gyro.core.GyroUI;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class ReplaceAction extends Action {

    private final Node resource;
    private final Node with;

    public ReplaceAction(Node resource, Node with) {
        this.resource = Preconditions.checkNotNull(resource);
        this.with = Preconditions.checkNotNull(with);
    }

    public Node getResource() {
        return resource;
    }

    public Node getWith() {
        return with;
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

        Resource currentResource = visitResource(this.resource, current);
        String currentResourceKey = currentResource.primaryKey();

        Resource pendingResource = visitResource(this.resource, scope);
        String pendingResourceKey = pendingResource.primaryKey();

        Resource pendingWith = visitResource(this.with, scope);

        if (current.getWorkflowReplacedResources().containsKey(currentResourceKey)) {
            DiffableInternals.setModifiedIn(currentResource, null);
            toBeRemoved.add(pendingResourceKey);
            toBeRemoved.add(pendingWith.primaryKey());
            return;
        }

        Set<String> modifiedIn = DiffableInternals.getModifiedIn(currentResource);

        if (modifiedIn == null) {
            modifiedIn = new LinkedHashSet<>();
            modifiedIn.add(Workflow.MAIN_RESOURCE);
        }
        modifiedIn.add(workflow.getType());

        current.getWorkflowReplacedResources().putIfAbsent(currentResourceKey, currentResource);

        Resource currentWith = visitResource(this.with, current);
        current.getWorkflowRemovedResources().putIfAbsent(currentWith.primaryKey(), currentWith);
        current.getWorkflowRemovedResources().putIfAbsent(currentResourceKey, currentResource);

        DiffableInternals.setModifiedIn(pendingResource, modifiedIn);

        DiffableInternals.setModifiedIn(pendingWith, modifiedIn);

        DiffableInternals.setModifiedIn(currentResource, modifiedIn);

        toBeReplaced.add(new ReplaceResource(pendingResource, pendingWith));
        toBeRemoved.add(pendingResourceKey);
    }
}
