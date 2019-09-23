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

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

@Type("type-description")
public class TypeDescriptionDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        List<Node> arguments = validateArguments(node, 2, 2);
        String type = (String) scope.getEvaluator().visit(arguments.get(0), scope);

        scope.getSettings(DescriptionSettings.class).getTypeDescriptions().put(type, arguments.get(1));
    }

}
