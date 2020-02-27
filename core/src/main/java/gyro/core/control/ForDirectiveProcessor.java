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

package gyro.core.control;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.util.CascadingMap;

@Type("for")
public class ForDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 0);

        List<String> variables = getArguments(scope, node, String.class);
        List<Node> inArguments = validateOptionArguments(node, "in", 1, 1);
        Node inNode = inArguments.get(0);
        Object in = scope.getRootScope().getEvaluator().visit(inNode, scope);

        if (in == null) {
            return;
        }

        if (in instanceof List || in instanceof Set) {
            List<?> list = in instanceof List ? (List<?>) in : new ArrayList<>((Set<?>) in);
            int variablesSize = variables.size();
            int listSize = list.size();

            for (int i = 0; i < listSize; i += variablesSize) {
                Map<String, Object> values = new LinkedHashMap<>();

                for (int j = 0; j < variablesSize; j++) {
                    int k = i + j;

                    values.put(
                        variables.get(j),
                        k < listSize
                            ? list.get(k)
                            : null);
                }

                processBody(node, scope, values);
            }

        } else if (in instanceof Map) {
            String keyVariable = variables.get(0);

            if (variables.size() > 1) {
                String valueVariable = variables.get(1);

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) in).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    values.put(valueVariable, entry.getValue());
                    processBody(node, scope, values);
                }

            } else {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) in).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    processBody(node, scope, values);
                }
            }

        } else {
            throw new GyroException(inNode, String.format(
                "Can't iterate over @|bold %s|@ which resolved to @|bold %s|@ because it's not a collection!",
                inNode,
                in));
        }
    }

    private void processBody(DirectiveNode node, Scope scope, Map<String, Object> values) {
        scope.getRootScope().getEvaluator().evaluateBody(
            node.getBody(),
            new Scope(scope, new CascadingMap<>(scope, values)));
    }

}
