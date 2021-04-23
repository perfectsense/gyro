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

package gyro.core.auth;

import java.util.Collections;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.value.ValueNode;

@Type("uses-credentials")
public class UsesCredentialsDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        String argument = getArgument(scope, node, String.class, 0);
        RootScope root = scope.getRootScope();
        
        if (scope instanceof DiffableScope) {
            DiffableScope diffableScope = (DiffableScope) scope;
            diffableScope.getSettings(CredentialsSettings.class)
                .setUseCredentials(argument);
            
            DirectiveNode stateNode = new DirectiveNode(
                node.getName(),
                Collections.singletonList(new ValueNode(argument)),
                node.getOptions(),
                node.getBody(),
                node.getSections());
            
            if (diffableScope.getStateNodes().stream()
                .filter(o -> o instanceof DirectiveNode)
                .map(o -> ((DirectiveNode) o))
                .noneMatch(o -> o.getName().equals(node.getName()))) {
                diffableScope.getStateNodes().add(stateNode);
            }
        } else {
            root.getSettings(CredentialsSettings.class).setUseCredentials(argument);
        }
    }

}
