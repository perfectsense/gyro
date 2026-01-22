/*
 * Copyright 2025, Perfect Sense, Inc.
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
import gyro.core.GyroUI;
import gyro.core.diff.Change;
import gyro.core.diff.Diff;
import gyro.core.scope.RootScope;
import gyro.core.scope.State;
import picocli.CommandLine.Command;

@Command(name = "refresh-state",
    synopsisHeading = "%n",
    header = "Refresh state from cloud providers without changing resources.",
    descriptionHeading = "%nDescription:%n%n",
    description = "Refreshes Gyro state with cloud resources upstream, "
        + "without creating, updating, or deleting resources.",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    usageHelpWidth = 100,
    mixinStandardHelpOptions = true,
    versionProvider = VersionCommand.class
)
public class RefreshStateCommand extends AbstractConfigCommand {

    @Override
    public void doExecute(RootScope current, RootScope pending, State state) throws Exception {
        GyroUI ui = GyroCore.ui();

        ui.write("\n@|bold,white Refreshing state from providers...\n\n|@");

        Diff diff = new Diff(
            pending.findSortedResourcesIn(pending.getLoadFiles()),
            current.findSortedResourcesIn(current.getLoadFiles()));

        diff.diff();

        if (!diff.write(ui)) {
            ui.write("\n@|bold,green No changes.|@\n\n");
        } else {
            ui.write("\n@|bold,white Detected drift. Updating state file(s) only.|@\n\n");
        }

        for (Change change : diff.getChanges()) {
            state.update(change);
        }
        state.save();
    }
}
