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

import com.psddev.dari.util.StringUtils;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public abstract class Action {

    public abstract void execute(
        GyroUI ui,
        State state,
        Scope scope,
        List<String> toBeRemoved,
        List<ReplaceResource> toBeReplaced,
        Workflow workflow);

    Resource visitResource(Node node, Scope scope) {
        Object resource = scope.getRootScope().getEvaluator().visit(node, scope);

        if (resource == null) {
            throw new GyroException(String.format("Can't %s a null resource!", getActionName()));
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't %s @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                getActionName(),
                resource,
                resource.getClass().getName()));
        }
        return (Resource) resource;
    }

    private String getActionName() {
        return StringUtils.removeEnd(getClass().getSimpleName(), "Action").toLowerCase();
    }
}
