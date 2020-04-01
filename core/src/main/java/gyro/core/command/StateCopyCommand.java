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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.IoUtils;
import gyro.core.FileBackend;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.GyroUI;
import gyro.core.LocalFileBackend;
import gyro.util.Bug;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.Option;
import io.airlift.airline.model.MetadataLoader;

@Command(name = "copy", description = "Copy state files between local and remote backend.")
public class StateCopyCommand implements GyroCommand {

    @Option(name = "--to-local", description = "Pull state files from a remote backend and copy to local backend")
    private boolean toLocal;

    @Option(name = "--to-remote", description = "Push state files from local backend and copy to a remote backend")
    private boolean toRemote;

    public boolean getToLocal() {
        return toLocal;
    }

    public boolean getToRemote() {
        return toRemote;
    }

    @Override
    public void execute() throws Exception {
        if ((getToLocal() && getToRemote()) || (!getToLocal() && !getToRemote())) {
            Help.help(MetadataLoader.loadCommand(StateCopyCommand.class));
            return;
        }

        FileBackend remoteStateBackend = GyroCore.getFileBackend(GyroCore.STATE_BACKEND);

        if (remoteStateBackend == null) {
            throw new GyroException(
                "Could not find a 'gyro-state' file backend in the 'init.gyro'! Add a file backend with the name 'gyro-state' to copy state files to/from.");
        }

        Path rootDir = GyroCore.getRootDirectory();

        if (rootDir == null) {
            throw new GyroException(
                "Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.");
        }

        LocalFileBackend localStateBackend = new LocalFileBackend(rootDir.resolve(".gyro/state"));
        boolean copiedFiles;

        if (getToLocal()) {
            GyroCore.ui().write("\n@|bold,white Looking for remote state files...|@\n\n");
            copiedFiles = copyBackends(remoteStateBackend, localStateBackend);
        } else {
            GyroCore.ui().write("\n@|bold,white Looking for local state files...|@\n\n");
            copiedFiles = copyBackends(localStateBackend, remoteStateBackend);
        }

        if (copiedFiles) {
            GyroCore.ui().write("\n@|bold,green OK|@\n");
        }
    }

    private static boolean copyBackends(FileBackend inputBackend, FileBackend outputBackend) {
        LinkedHashSet<String> files = list(inputBackend)
            .filter(f -> f.endsWith(".gyro") && !f.contains(GyroCore.INIT_FILE))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (files.isEmpty()) {
            GyroCore.ui().write("\n@|bold,green No state files found.|@\n");
            return false;
        }

        GyroUI ui = GyroCore.ui();

        files.forEach(file -> ui.write("@|green + Copy file: %s|@\n", file));

        if (!ui.readBoolean(
            Boolean.FALSE,
            "\nAre you sure you want to copy all files? @|red This will overwrite existing files!|@")) {
            return false;
        }

        files.forEach(file -> {
            ui.write("@|magenta + Copying file: %s|@\n", file);
            try (OutputStream out = openOutput(outputBackend, file)) {
                InputStream in = openInput(inputBackend, file);
                IoUtils.copy(in, out);
            } catch (IOException error) {
                throw new Bug(error);
            }
        });

        return true;
    }

    private static GyroInputStream openInput(FileBackend fileBackend, String file) {
        return new GyroInputStream(fileBackend, file);
    }

    private static GyroOutputStream openOutput(FileBackend fileBackend, String file) {
        return new GyroOutputStream(fileBackend, file);
    }

    private static Stream<String> list(FileBackend fileBackend) {
        try {
            return fileBackend.list();

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't list files in @|bold %s|@!", fileBackend),
                error);
        }
    }
}
