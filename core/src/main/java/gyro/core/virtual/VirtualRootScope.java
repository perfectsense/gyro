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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;

public class VirtualRootScope extends RootScope {

    private final String virtualName;
    private final RootScope current;

    public VirtualRootScope(RootScope scope, String virtualName) {
        super(scope.getFile(), scope.getBackend(), null, scope.getLoadFiles());
        this.virtualName = virtualName;
        this.current = scope;
        putAll(scope);
        getFileScopes().addAll(scope.getFileScopes());
    }

    @Override
    public Resource findResource(String fullName) {
        String name = fullName;
        if (!fullName.contains("/")) {
            String[] names = fullName.split("::");
            names[names.length - 1] = virtualName + "/" + names[names.length - 1];
            name = String.join("::", names);
        }

        return current.findResource(name);
    }

    @Override
    public List<Resource> findSortedResources() {
        return current.findSortedResources().stream()
            .filter(r -> DiffableInternals.getName(r).startsWith(virtualName))
            .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findSortedResourcesIn(Set<String> diffFiles) {
        return current.findSortedResourcesIn(diffFiles).stream()
            .filter(r -> DiffableInternals.getName(r).startsWith(virtualName))
            .collect(Collectors.toList());
    }

    @Override
    public <T extends Resource> Stream<T> findResourcesByClass(Class<T> resourceClass) {
        return current.findResourcesByClass(resourceClass)
            .filter(r -> DiffableInternals.getName(r).startsWith(virtualName));
    }

    @Override
    public void evaluate() {
        throw new UnsupportedOperationException();
    }

}
