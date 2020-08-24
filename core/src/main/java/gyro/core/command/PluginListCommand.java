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

import java.util.Collections;
import java.util.List;

import gyro.core.GyroCore;
import picocli.CommandLine.Command;

@Command(name = "list",
    header = "List plugins installed on this project.",
    synopsisHeading = "%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n"
)
public class PluginListCommand extends PluginCommand {

    @Override
    public List<String> getPlugins() {
        return Collections.emptyList();
    }

    @Override
    protected void executeSubCommand() {
        GyroCore.ui().write("\n");

        getPluginNodes()
            .stream()
            .map(this::toPluginString)
            .map(p -> String.format("@|bold %s|@%n", p))
            .forEach(GyroCore.ui()::write);
    }
}
