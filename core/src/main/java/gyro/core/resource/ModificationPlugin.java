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

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;
import net.jodah.typetools.TypeResolver;

public class ModificationPlugin extends Plugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Modification.class.isAssignableFrom(aClass)) {
            Class<? extends Modification<Diffable>> modificationClass = (Class<? extends Modification<Diffable>>) aClass;
            Class<?> diffableClass = TypeResolver.resolveRawArgument(Modification.class, modificationClass);
            DiffableType<Diffable> type = DiffableType.getInstance((Class<Diffable>) diffableClass);

            type.modify(modificationClass);
        }
    }

}
