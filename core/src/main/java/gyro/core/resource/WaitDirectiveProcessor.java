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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import gyro.core.Type;
import gyro.core.Waiter;
import gyro.core.diff.ChangeProcessor;
import gyro.core.diff.ChangeSettings;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

@Type("wait")
public class WaitDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        validateOptionArguments(node, "unit", 0, 1);
        validateOptionArguments(node, "at-most", 0, 1);
        validateOptionArguments(node, "check-every", 0, 1);

        Waiter waiter = new Waiter();

        TimeUnit unit = Optional.ofNullable(getOptionArgument(scope, node, "unit", TimeUnit.class, 0))
            .orElse(TimeUnit.SECONDS);

        Optional.ofNullable(getOptionArgument(scope, node, "at-most", Long.class, 0))
            .ifPresent(d -> waiter.atMost(d, unit));

        Optional.ofNullable(getOptionArgument(scope, node, "check-every", Long.class, 0))
            .ifPresent(d -> waiter.checkEvery(d, unit));

        boolean found = false;
        Node condition = node.getArguments().get(0);
        List<ChangeProcessor> processors = scope.getSettings(ChangeSettings.class).getProcessors();

        for (ChangeProcessor processor : processors) {
            // This directive processor is visited multiple times by {@link NodeEvaluator#evaluateDiffable}.
            if (processor instanceof WaitChangeProcessor
                && ((WaitChangeProcessor) processor).getParent().equals(scope)
                && ((WaitChangeProcessor) processor).getCondition().equals(condition)) {
                found = true;
                break;
            }
        }

        if (!found) {
            processors.add(new WaitChangeProcessor(scope, waiter, condition));
        }
    }
}
