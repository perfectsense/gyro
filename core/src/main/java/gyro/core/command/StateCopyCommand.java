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
import java.util.UUID;
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
import gyro.core.LockBackend;
import gyro.util.Bug;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "copy", description = "Copy state files between named backends. Use 'local' to copy to/from your local directory. Use 'default' to copy to/from an unnamed backend in your 'init.gyro'.")
public class StateCopyCommand implements GyroCommand {

    @Option(name = "--to", description = "Specifies the name of the backend to push state files to", required = true)
    private String to;

    @Option(name = "--from", description = "Specifies the name of the backend to pull state files from", required = true)
    private String from;

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    @Override
    public void execute() throws Exception {
        if (getTo().equals(getFrom())) {
            throw new GyroException(String.format("Cannot specify '%s' backend for both to and from!", getTo()));
        }

        Path rootDir = GyroCore.getRootDirectory();

        if (rootDir == null) {
            throw new GyroException(
                "Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.");
        }

        LockBackend lockBackend = GyroCore.getLockBackend();

        if (lockBackend != null) {
            lockBackend.setLockId(UUID.randomUUID().toString());
            lockBackend.lock();
        }

        FileBackend toBackend;
        FileBackend fromBackend;

        if ("local".equals(getTo())) {
            toBackend = new LocalFileBackend(rootDir.resolve(".gyro/state"));
        } else {
            toBackend = GyroCore.getStateBackend(getTo());
        }

        if ("local".equals(getFrom())) {
            fromBackend = new LocalFileBackend(rootDir.resolve(".gyro/state"));
        } else {
            fromBackend = GyroCore.getStateBackend(getFrom());
        }

        if (toBackend == null || fromBackend == null) {
            throw new GyroException(String.format(
                "Could not find specified state-backend '%s' in '.gyro/init.gyro'!",
                toBackend != null ? getFrom() : getTo()));
        }

        boolean copiedFiles;

        GyroCore.ui().write("\n@|bold,white Looking for state files...|@\n\n");

        try {
            copiedFiles = copyBackends(fromBackend, toBackend, false, true);
        } finally {
            if (lockBackend != null) {
                lockBackend.unlock();
            }
        }

        if (copiedFiles) {
            GyroCore.ui().write(String.format(
                "\n@|bold,green State files copied.|@ You may now delete files from '%s' if they are no longer needed.\n",
                getFrom()));
        }
    }

    public static boolean copyBackends(
        FileBackend inputBackend,
        FileBackend outputBackend,
        boolean deleteInput,
        boolean displayMessaging) {
        GyroUI ui = GyroCore.ui();
        LinkedHashSet<String> files = list(inputBackend)
            .filter(f -> f.endsWith(".gyro") && !f.contains(GyroCore.INIT_FILE))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (files.isEmpty()) {
            if (displayMessaging) {
                ui.write("\n@|bold,green No state files found.|@\n");
            }
            return false;
        }

        if (displayMessaging) {
            files.forEach(file -> ui.write("@|green + Copy file: %s|@\n", file));

            if (!ui.readBoolean(
                Boolean.FALSE,
                "\nAre you sure you want to copy all files? @|red This will overwrite existing files!|@")) {
                return false;
            }
        }

        files.forEach(file -> {
            if (displayMessaging) {
                ui.write("@|magenta + Copying file: %s|@\n", file);
            }

            try (OutputStream out = new GyroOutputStream(outputBackend, file)) {
                InputStream in = new GyroInputStream(inputBackend, file);
                IoUtils.copy(in, out);
            } catch (IOException error) {
                throw new Bug(error);
            }

            if (deleteInput) {
                delete(inputBackend, file);
            }
        });

        return true;
    }

    private static void delete(FileBackend fileBackend, String file) {
        try {
            fileBackend.delete(file);

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't delete @|bold %s|@ in @|bold %s|@!", file, fileBackend),
                error);
        }
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
