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

package gyro.core.virtual;

import java.util.List;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.FileScope;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.RootScope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.ResourceNode;

@Type("virtual")
public class VirtualDirectiveProcessor extends DirectiveProcessor<FileScope> {

    @Override
    public void process(FileScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        RootScope root = scope.getRootScope();
        String type = getArgument(scope, node, String.class, 0);
        List<Node> body = node.getBody();

        root.put(type, new VirtualResourceVisitor(scope, body));

        // When virtual directive defines other resources, wildcard queries to those types of resources should defer
        // until all virtual resources are actually created.
        NodeEvaluator evaluator = root.getEvaluator();

        evaluator.getBody()
            .stream()
            .filter(FileNode.class::isInstance)
            .map(FileNode.class::cast)
            .map(FileNode::getBody)
            .flatMap(List::stream)
            .filter(ResourceNode.class::isInstance)
            .map(ResourceNode.class::cast)
            .filter(r -> type.equals(r.getType()))
            .forEach(vr -> body.forEach(i -> evaluator.addTypeNode(vr, i)));
    }

}
