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

import gyro.core.plugin.Plugin;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;

public class FinderPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Finder.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Finder<Resource>> finderClass = (Class<? extends Finder<Resource>>) aClass;

            root.getSettings(FinderSettings.class).getFinderClasses().put(
                FinderType.getInstance(finderClass).getName(),
                finderClass);
        }
    }

}
