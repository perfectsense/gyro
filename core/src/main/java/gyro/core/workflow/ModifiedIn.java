/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.workflow;

public enum ModifiedIn {
    WORKFLOW_ONLY,
    BOTH;

    @Override
    public String toString() {
        return name();
    }

    public static ModifiedIn fromString(String name) {
        for (ModifiedIn value : ModifiedIn.values()) {
            if (value.toString().equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("no value found:" + name);
    }
}
