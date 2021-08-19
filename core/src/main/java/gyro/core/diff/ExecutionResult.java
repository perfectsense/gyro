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

package gyro.core.diff;

import gyro.core.GyroUI;

public enum ExecutionResult {

    OK {
        public void write(GyroUI ui) {
            ui.write(ui.isVerbose() ? "@|bold,green OK|@\n\n" : " @|bold,green OK|@\n");
        }
    },

    SKIPPED {
        public void write(GyroUI ui) {
            ui.write(ui.isVerbose() ? "@|bold,yellow SKIPPED|@\n\n" : " @|bold,yellow SKIPPED|@\n");
        }
    };

    public abstract void write(GyroUI ui);

}
