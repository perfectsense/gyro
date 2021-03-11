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
import gyro.core.GyroUI;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class ReplaceAction extends Action {

    private final Scope scope;
    private final Node resource;
    private final Node with;

    public ReplaceAction(Scope scope, Node resource, Node with) {
        this.scope = Preconditions.checkNotNull(scope);
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
        Scope stageScope,
        List<String> toBeRemoved,
        List<ReplaceResource> toBeReplaced) {

        Scope actionScope = new Scope(stageScope);

        for (Map.Entry<String, Object> entry : scope.entrySet()) {
            Object value = entry.getValue();

            if (!(value instanceof Resource)) {
                actionScope.put(entry.getKey(), value);
            }
        }

        RootScope pending = actionScope.getRootScope();
        RootScope current = pending.getCurrent();

        for (Scope s = actionScope, p; (p = s.getParent()) != null; s = p) {
            if (p instanceof RootScope) {
                s.setParent(current);
            }
        }
        Resource currentResource = visitResource(this.resource, actionScope);
        String currentResourceKey = currentResource.primaryKey();
        for (Scope s = actionScope, p; (p = s.getParent()) != null; s = p) {
            if (p instanceof RootScope) {
                s.setParent(pending);
            }
        }

        Resource pendingResource = visitResource(this.resource, actionScope);
        String pendingResourceKey = pendingResource.primaryKey();

        Resource pendingWith = visitResource(this.with, actionScope);

        if (current.getWorkflowReplacedResources().containsKey(currentResourceKey)) {
            DiffableInternals.setModifiedIn(currentResource, null);
            toBeRemoved.add(pendingResourceKey);
            toBeRemoved.add(pendingWith.primaryKey());
            return;
        }

        ModifiedIn modifiedIn = DiffableInternals.getModifiedIn(currentResource) == ModifiedIn.WORKFLOW_ONLY
            ? ModifiedIn.WORKFLOW_ONLY
            : ModifiedIn.BOTH;
        current.getWorkflowReplacedResources().putIfAbsent(currentResourceKey, currentResource);

        for (Scope s = actionScope, p; (p = s.getParent()) != null; s = p) {
            if (p instanceof RootScope) {
                s.setParent(current);
            }
        }
        Resource currentWith = visitResource(this.with, actionScope);
        for (Scope s = actionScope, p; (p = s.getParent()) != null; s = p) {
            if (p instanceof RootScope) {
                s.setParent(pending);
            }
        }
        current.getWorkflowRemovedResources().putIfAbsent(currentWith.primaryKey(), currentWith);
        current.getWorkflowRemovedResources().putIfAbsent(currentResourceKey, currentResource);

        DiffableInternals.setModifiedIn(pendingResource, modifiedIn);

        DiffableInternals.setModifiedIn(pendingWith, modifiedIn);

        DiffableInternals.setModifiedIn(currentResource, modifiedIn);

        toBeReplaced.add(new ReplaceResource(pendingResource, pendingWith));
        toBeRemoved.add(pendingResourceKey);
    }
}
