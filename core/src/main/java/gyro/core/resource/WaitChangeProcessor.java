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

package gyro.core.resource;

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.Waiter;
import gyro.core.diff.ChangeProcessor;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class WaitChangeProcessor extends ChangeProcessor {

    private final DiffableScope parent;
    private final Waiter waiter;
    private final Node condition;

    public WaitChangeProcessor(DiffableScope parent, Waiter waiter, Node condition) {
        this.parent = parent;
        this.waiter = waiter;
        this.condition = condition;
    }

    public DiffableScope getParent() {
        return parent;
    }

    public Node getCondition() {
        return condition;
    }

    private void wait(GyroUI ui, State state, Resource resource) {
        ui.write("\n");

        ui.indented(() -> {
            if (state.isTest()) {
                ui.write("@|magenta ⧖ Waiting skipped because in test mode|@");
                return;
            }

            ui.write("@|magenta ⧖ Waiting for: %s|@\n", condition);

            NodeEvaluator evaluator = parent.getRootScope().getEvaluator();
            ObjectScope scope = new ObjectScope(parent, resource);

            ui.indented(() ->
                waiter.until(() -> {
                    ui.write("@|magenta ✓ Checking |@");

                    boolean result = Boolean.TRUE.equals(evaluator.visit(condition, scope));

                    ui.write(result ? "@|green PASSED|@" : "@|red FAILED|@\n");
                    return result;
                })
            );
        });
    }

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) {
        wait(ui, state, resource);
    }

    @Override
    public void afterUpdate(
        GyroUI ui,
        State state,
        Resource current,
        Resource pending,
        Set<DiffableField> changedFields) {
        wait(ui, state, pending);
    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) {
        wait(ui, state, resource);
    }

}
