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

import gyro.core.GyroException;
import gyro.core.scope.Scope;

public class VirtualParameter {

    private final String name;

    public VirtualParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void copy(Scope source, Scope destination) {
        if (!source.containsKey(name)) {
            throw new GyroException(String.format(
                "@|bold %s|@ parameter is required!",
                name));

        } else {
            destination.put(name, source.get(name));
        }
    }

}
