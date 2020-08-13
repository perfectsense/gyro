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

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "init", description = "Initialize a Gyro working directory.")
public class InitCommand extends AbstractCommand {

    @Parameters(description = "A list of plugins specified in the format of <group>:<artifact>:<version>. "
        + "For example: gyro:gyro-aws-provider:0.1-SNAPSHOT")
    private List<String> plugins;

    @Override
    protected void doExecute() throws Exception {
        if (plugins == null || plugins.isEmpty()) {
            throw new GyroException("List of plugins is required!");
        }

        for (String plugin : plugins) {
            if (plugin.split(":").length != 3) {
                throw new GyroException(String.format(
                    "[%s] isn't properly formatted!",
                    plugin));
            }
        }

        Path gyroDir = Paths.get(".gyro");

        if (!Files.exists(gyroDir)) {
            GyroCore.ui().write("@|magenta + Creating a new .gyro directory|@\n");
            Files.createDirectories(gyroDir);

        } else if (Files.isDirectory(gyroDir)) {
            GyroCore.ui().write("This Gyro project directory has already been initialized.\n");
            return;

        } else {
            throw new GyroException("Can't create the .gyro directory because there's a file there!");
        }

        GyroCore.ui().write("@|magenta + Writing to the .gyro/init.gyro file|@\n");

        try (BufferedWriter writer = Files.newBufferedWriter(gyroDir.resolve("init.gyro"), StandardCharsets.UTF_8)) {
            writer.write("@repository: 'https://artifactory.psdops.com/public'\n");
            writer.write("@repository: 'https://artifactory.psdops.com/gyro-snapshots'\n");

            for (String plugin : plugins) {
                writer.write("@plugin: '");
                writer.write(plugin);
                writer.write("'\n");
            }
        }
    }
}
