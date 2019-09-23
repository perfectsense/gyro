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

package gyro.core.scope;

import java.util.List;
import java.util.Set;

import gyro.core.GyroUI;

class CircularDefer extends Defer {

    private final Set<CreateDefer> errors;
    private final List<Defer> related;

    public CircularDefer(Set<CreateDefer> errors, List<Defer> related) {
        super(null, null, null);

        this.errors = errors;
        this.related = related;
    }

    @Override
    public void write(GyroUI ui) {
        writeErrors(ui, "@|red Circular dependency detected!|@\n", errors);
        writeErrors(ui, "\n@|red Related:|@\n", related);
    }

}
