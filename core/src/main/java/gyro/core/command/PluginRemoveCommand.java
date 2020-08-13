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

import java.util.List;
import java.util.stream.Collectors;

import gyro.core.GyroCore;
import gyro.lang.ast.block.DirectiveNode;
import picocli.CommandLine.Command;

@Command(name = "remove", description = "Remove one or more plugins.")
public class PluginRemoveCommand extends PluginCommand {

    @Override
    protected void executeSubCommand() throws Exception {
        if (getPlugins().isEmpty()) {
            //Help.help(MetadataLoader.loadCommand(PluginRemoveCommand.class));
            return;
        }

        List<DirectiveNode> removeNodes = getPluginNodes()
            .stream()
            .filter(this::pluginNodeExist)
            .collect(Collectors.toList());

        List<DirectiveNode> invalidNodes = getPluginNodes()
            .stream()
            .filter(f -> !pluginNodeExist(f))
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        int lineNumber = 0;
        for (String line : load()) {
            boolean skipLine = false;
            for (DirectiveNode pluginNode : removeNodes) {
                if (lineNumber >= pluginNode.getStartLine() && lineNumber <= pluginNode.getStopLine()) {
                    skipLine = true;
                }
            }

            if (!skipLine) {
                sb.append(line);
                sb.append("\n");
            }

            lineNumber++;
        }

        save(sb.toString());

        GyroCore.ui().write("\n");

        invalidNodes.stream()
            .map(this::toPluginString)
            .map(p -> String.format("@|bold %s|@ was not installed.%n", p))
            .forEach(GyroCore.ui()::write);

        removeNodes.stream()
            .map(this::toPluginString)
            .map(p -> String.format("@|bold %s|@ has been removed.%n", p))
            .forEach(GyroCore.ui()::write);
    }
}
