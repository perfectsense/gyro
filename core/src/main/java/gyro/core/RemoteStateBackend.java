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

package gyro.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.IoUtils;
import gyro.util.Bug;

public class RemoteStateBackend {

    private FileBackend remoteBackend;
    private LocalFileBackend localBackend;

    public FileBackend getRemoteBackend() {
        return remoteBackend;
    }

    public FileBackend getLocalBackend() {
        return localBackend;
    }

    public RemoteStateBackend(FileBackend remoteBackend, LocalFileBackend localBackend) {
        this.remoteBackend = remoteBackend;
        this.localBackend = localBackend;
    }

    public boolean isLocalBackendEmpty() {
        return list(localBackend).noneMatch(f -> f.endsWith(".gyro"));
    }

    public void deleteLocalBackend() {
        localBackend.deleteDirectory();
    }

    public boolean copyToRemote(boolean deleteInput, boolean displayMessaging) {
        boolean copyBackends = copyBackends(localBackend, remoteBackend, deleteInput, displayMessaging);

        if (deleteInput) {
            deleteLocalBackend();
        }

        return copyBackends;
    }

    public boolean copyToLocal(boolean deleteInput, boolean displayMessaging) {
        boolean copyBackends = copyBackends(remoteBackend, localBackend, deleteInput, displayMessaging);

        if (deleteInput) {
            deleteLocalBackend();
        }

        return copyBackends;
    }

    private static boolean copyBackends(
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
