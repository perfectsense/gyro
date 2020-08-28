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

package gyro.core.command;

import picocli.CommandLine;

@CommandLine.Command(name = "plugin",
    description = "Manage gyro plugins.",
    synopsisHeading = "%n",
    header = "Add, remove, or list plugins defined in .gyro/init.gyro.",
    descriptionHeading = "%nDescription:%n%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    commandListHeading = "%nCommands:%n",
    usageHelpWidth = 100,
    mixinStandardHelpOptions = true,
    versionProvider = VersionCommand.class,
    subcommands = {
        PluginListCommand.class,
        PluginAddCommand.class,
        PluginRemoveCommand.class
    }
)
public class PluginCommandGroup implements GyroCommandGroup {

}
