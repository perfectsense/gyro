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

import java.util.HashMap;
import java.util.Map;

import gyro.core.resource.Resource;
import gyro.core.scope.Settings;

public class FinderSettings extends Settings {

    private Map<String, Class<? extends Finder<Resource>>> finderClasses;

    public Map<String, Class<? extends Finder<Resource>>> getFinderClasses() {
        if (finderClasses == null) {
            finderClasses = new HashMap<>();
        }

        return finderClasses;
    }

    public void setFinderClasses(Map<String, Class<? extends Finder<Resource>>> finderClasses) {
        this.finderClasses = finderClasses;
    }

}
