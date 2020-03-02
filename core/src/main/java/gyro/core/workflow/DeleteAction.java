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

import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.NodeEvaluator;
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
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        NodeEvaluator evaluator = pending.getEvaluator();
        Object resource = evaluator.visit(this.resource, pending);

        if (resource == null) {
            throw new GyroException("Can't delete a null resource!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't delete @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                resource.getClass().getName()));
        }

        String fullName = ((Resource) resource).primaryKey();

        pending.getFileScopes().forEach(s -> s.remove(fullName));
    }

}
