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
import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.ResourceNode;

public class CreateAction extends Action {

    private final Node type;
    private final Node name;
    private final List<Node> body;

    public CreateAction(Node type, Node name, List<Node> body) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public Node getType() {
        return type;
    }

    public Node getName() {
        return name;
    }

    public List<Node> getBody() {
        return body;
    }

    @Override
    public void execute(GyroUI ui, State state, Scope scope) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        evaluator.visit(
            new ResourceNode(
                (String) evaluator.visit(type, scope),
                name,
                body),
            scope);
    }
}
