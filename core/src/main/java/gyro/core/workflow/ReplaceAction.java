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

import com.google.common.base.Preconditions;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.NodeEvaluator;
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
    public void execute(GyroUI ui, State state, Scope scope) {
        RootScope pending = scope.getRootScope();
        RootScope current = pending.getCurrent();
        NodeEvaluator evaluator = current.getEvaluator();
        Object resource = evaluator.visit(this.resource, current);

        if (resource == null) {
            throw new GyroException("Can't replace a null resource!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't replace @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                resource.getClass().getName()));
        }

        Object with = evaluator.visit(this.with, current);

        if (with == null) {
            throw new GyroException(String.format(
                "Can't @|bold %s|@ resource with a null!",
                resource));
        }

        if (!(with instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't @|bold %s|@ resource with @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                with,
                with.getClass().getName()));
        }

        Resource resourceResource = (Resource) resource;
        Resource withResource = (Resource) with;
        String resourceKey = resourceResource.primaryKey();

        ui.write("@|magenta ⤢ Replacing %s with %s|@\n", resourceKey, withResource.primaryKey());

        if (state != null) {
            state.replace(resourceResource, withResource);
        }

        pending.getFileScopes().forEach(s -> s.remove(resourceKey));
    }

}
