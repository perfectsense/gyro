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

package gyro.core.finder;

import java.util.List;
import java.util.Map;

import com.psddev.dari.util.TypeDefinition;
import gyro.core.auth.Credentials;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.Scope;

public abstract class Finder<R extends Resource> {

    Scope scope;

    public abstract List<R> findAll();

    public abstract List<R> find(Map<String, Object> filters);

    public <C extends Credentials> C credentials(Class<C> credentialsClass) {
        return Credentials.getInstance(credentialsClass, getClass(), scope);
    }

    public R newResource() {
        @SuppressWarnings("unchecked")
        Class<R> resourceClass = (Class<R>) TypeDefinition.getInstance(getClass())
            .getInferredGenericTypeArgumentClass(Finder.class, 0);

        return DiffableType.getInstance(resourceClass).newExternalWithCredentials(scope.getRootScope(), null);
    }

}
