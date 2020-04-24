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

import gyro.core.command.StateCopyCommand;

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
        try {
            return localBackend.list().noneMatch(f -> f.endsWith(".gyro"));
        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't list files in @|bold %s|@!", localBackend),
                error);
        }
    }

    public void deleteLocalBackend() {
        localBackend.deleteDirectory();
    }

    public void copyToRemote(boolean deleteInput, boolean displayMessaging) {
        StateCopyCommand.copyBackends(localBackend, remoteBackend, deleteInput, displayMessaging);

        if (deleteInput) {
            deleteLocalBackend();
        }
    }
}
