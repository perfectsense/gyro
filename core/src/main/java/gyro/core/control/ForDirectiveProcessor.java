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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Defer;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.block.BlockNode;
import gyro.lang.ast.block.DirectiveNode;
import gyro.util.CascadingMap;

@Type("for")
public class ForDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 0);

        List<String> variables = getArguments(scope, node, String.class);
        List<Node> inArguments = validateOptionArguments(node, "in", 1, 1);
        validateScopedVariables(scope, node, variables);

        Node inNode = inArguments.get(0);
        Object in = scope.getRootScope().getEvaluator().visit(inNode, scope);

        if (in == null) {
            return;
        }

        if (in instanceof List) {
            List<?> list = (List<?>) in;
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

    private void validateScopedVariables(Scope scope, DirectiveNode node, List<String> variables) {
        RootScope rootScope = scope.getRootScope();
        Set<String> globalScopedVariables = !scope.equals(rootScope) ? new HashSet<>(PairNode.getNodeVariables(scope.getRootScope().getNodes())) : new HashSet<>();
        Set<String> fileScopedVariables = scope.keySet();

        // variable check
        validateVariables(node, variables, fileScopedVariables, globalScopedVariables);

        // body check
        validateBody(node, variables, fileScopedVariables, globalScopedVariables);
    }

    private void validateVariables(DirectiveNode node, List<String> variables, Set<String> fileScopedVariables, Set<String> globalScopedVariables) {
        // duplicate inline variable
        String duplicate = BlockNode.validateLocalImmutability(variables);

        if (duplicate != null) {
            throw new Defer(node, String.format("duplicate inline variable '%s'!", duplicate));
        }

        validateGlobalAndFileScope(node, variables, fileScopedVariables, globalScopedVariables, false);
    }

    private void validateBody(DirectiveNode node, List<String> variables, Set<String> fileScopedVariables, Set<String> globalScopedVariables) {
        // duplicate body variable
        String duplicate = BlockNode.validateLocalImmutability(node);

        if (duplicate != null) {
            throw new Defer(PairNode.getKeyNode(node.getBody(), duplicate), String.format("duplicate for body variable '%s'!", duplicate));
        }

        List<String> bodyVariables = PairNode.getNodeVariables(node.getBody());

        // inline scoped variable defined as body variable
        duplicate = bodyVariables.stream().filter(variables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(PairNode.getKeyNode(node.getBody(), duplicate), String.format("'%s' is already defined inline and cannot be reused!", duplicate));
        }

        validateGlobalAndFileScope(node, bodyVariables, fileScopedVariables, globalScopedVariables, true);
    }

    private void validateGlobalAndFileScope(
        DirectiveNode node,
        List<String> variables,
        Set<String> fileScopedVariables,
        Set<String> globalScopedVariables,
        boolean isBody) {

        // file scoped variable defined as inline/body variable
        String duplicate = variables.stream().filter(fileScopedVariables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(isBody ? PairNode.getKeyNode(node.getBody(), duplicate) : node, String.format("'%s' is already defined in the file scope and cannot be reused!", duplicate));
        }

        // global scoped variable defined as inline/body variable
        duplicate = variables.stream().filter(globalScopedVariables::contains).findFirst().orElse(null);

        if (duplicate != null) {
            throw new Defer(isBody ? PairNode.getKeyNode(node.getBody(), duplicate) : node, String.format("'%s' is already defined in the global scope and cannot be reused!", duplicate));
        }
    }

}
