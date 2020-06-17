/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.replacer;

import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.WorkflowReplacer;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("workflow-replacer")
public class WorkflowReplacerDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        String type = getArgument(scope, node, String.class, 0);
        WorkflowReplacer workflowReplacer;

        if ("create-delete".equals(type)) {
            workflowReplacer = new CreateDeleteWorkflowReplacer();
        } else {
            RootScope rootScope = scope.getRootScope();
            Class<? extends WorkflowReplacer> replacerClass = rootScope.getSettings(WorkflowReplacerSettings.class)
                .getWorkflowReplacerClasses()
                .get(type);

            if (replacerClass == null) {
                throw new GyroException(String.format("Can't find a replacer of type '%s'!", type));
            }

            workflowReplacer = Reflections.newInstance(replacerClass);
        }

        workflowReplacer.setScope(scope);

        scope.getSettings(WorkflowReplacerSettings.class).setWorkflowReplacer(workflowReplacer);
    }

}
