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
import gyro.core.GyroUI;
import gyro.core.diff.Diff;
import gyro.core.scope.RootScope;
import gyro.core.scope.State;
import picocli.CommandLine.Command;

@Command(name = "diff",
    header = "Shows differences between the configuration and the cloud.",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    description = "Compare differences between gyro configuration and state. Any differences are displayed.",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    usageHelpWidth = 100,
    mixinStandardHelpOptions = true,
    versionProvider = VersionCommand.class
)
public class DiffCommand extends AbstractConfigCommand {

    @Override
    public void doExecute(RootScope current, RootScope pending, State state) throws Exception {
        GyroUI ui = GyroCore.ui();

        ui.write("\n@|bold,white Looking for changes...\n\n|@");

        Diff diff = new Diff(
            current.findSortedResourcesIn(current.getLoadFiles()),
            pending.findSortedResourcesIn(pending.getLoadFiles()));

        diff.diff();

        if (!diff.write(ui)) {
            ui.write("\n@|bold,green No changes.|@\n\n");
        }
    }
}
