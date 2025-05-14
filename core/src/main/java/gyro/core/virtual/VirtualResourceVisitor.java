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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceVisitor;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class VirtualResourceVisitor extends ResourceVisitor {

    private final List<VirtualParameter> parameters;
    private final List<Node> body;

    public VirtualResourceVisitor(Scope scope, List<Node> body) {
        ImmutableList.Builder<VirtualParameter> parametersBuilder = ImmutableList.builder();
        ImmutableList.Builder<Node> bodyBuilder = ImmutableList.builder();

        for (Node child : body) {
            if (child instanceof DirectiveNode) {
                DirectiveNode directive = (DirectiveNode) child;

                if ("param".equals(directive.getName())) {
                    DirectiveProcessor.validateArguments(directive, 1, 1);
                    parametersBuilder.add(new VirtualParameter(DirectiveProcessor.getArgument(
                        scope,
                        directive,
                        String.class,
                        0)));
                    continue;
                }
            }

            bodyBuilder.add(child);
        }

        this.parameters = parametersBuilder.build();
        this.body = bodyBuilder.build();
    }

    public List<VirtualParameter> getParameters() {
        return parameters;
    }

    public List<Node> getBody() {
        return body;
    }

    @Override
    public void visit(String name, Scope scope) {
        RootScope root = scope.getRootScope();

        RootScope virtualRoot = new RootScope(
            root.getFile(),
            root.getBackend(),
            root.getRemoteStateBackend(),
            new VirtualRootScope(root.getCurrent(), name),
            root.getLoadFiles());

        virtualRoot.getSettingsByClass().putAll(root.getSettingsByClass().asMap());
        virtualRoot.getCurrent().getSettingsByClass().putAll(root.getSettingsByClass().asMap());
        virtualRoot.getCurrent().getRootScope().getSettingsByClass().putAll(root.getSettingsByClass().asMap());
        virtualRoot.putAll(root);

        Map<String, Resource> currentResources = root.getResources();
        Set<String> resourceKeys = currentResources.keySet();
        virtualRoot.putAll(currentResources);

        FileScope file = scope.getFileScope();
        FileScope virtualFile = new FileScope(virtualRoot, file.getFile());

        virtualRoot.getFileScopes().add(virtualFile);
        parameters.forEach(p -> p.copy(scope, virtualFile));
        virtualRoot.getEvaluator().evaluateBody(body, virtualFile);

        String prefix = name + "/";

        List<Resource> newSortedResources = virtualRoot.findSortedResources().stream()
            .filter(r -> !resourceKeys.contains(r.primaryKey())).collect(Collectors.toList());

        for (Resource resource : newSortedResources) {
            DiffableInternals.setName(resource, prefix + DiffableInternals.getName(resource));
            file.put(resource.primaryKey(), resource);
        }
    }
}
