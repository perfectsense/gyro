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

package gyro.core.command;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LockBackend;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "force-unlock",
    header = "Unlock the current state backend lock.",
    synopsisHeading = "%n",
    description = "Use with caution. ",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n%n"
)
public class StateForceUnlockCommand implements GyroCommand {

    @Option(names = "--lock-id", description = "The ID of the current lock you wish to unlock", required = true)
    private String lockId;

    @Override
    public void execute() throws Exception {
        LockBackend lockBackend = GyroCore.getLockBackend();

        if (lockBackend == null) {
            throw new GyroException("Cannot find a lock backend configured in 'init.gyro'!");
        }

        lockBackend.unlock(lockId);

        GyroCore.ui().write("\n@|bold,green State unlocked.|@\n");
    }
}
