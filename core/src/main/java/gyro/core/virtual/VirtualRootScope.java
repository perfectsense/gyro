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

import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;

public class VirtualRootScope extends RootScope {

    private final String virtualName;

    public VirtualRootScope(RootScope scope, String virtualName) {
        super(scope.getFile(), scope.getBackend(), scope.getStateBackend(), null, scope.getLoadFiles());
        this.virtualName = virtualName;
        putAll(scope);
        getFileScopes().addAll(scope.getFileScopes());
    }

    @Override
    public Resource findResource(String fullName) {
        String[] names = fullName.split("::");
        names[names.length - 1] = virtualName + "/" + names[names.length - 1];
        return super.findResource(String.join("::", names));
    }

    @Override
    public void evaluate() {
        throw new UnsupportedOperationException();
    }

}
