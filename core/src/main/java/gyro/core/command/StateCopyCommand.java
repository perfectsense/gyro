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

import java.nio.file.Path;

import gyro.core.FileBackend;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.core.RemoteStateBackend;
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

        FileBackend remoteBackend = GyroCore.getFileBackend(GyroCore.STATE_BACKEND);

        if (remoteBackend == null) {
            throw new GyroException(
                "Could not find a 'gyro-state' file backend in the 'init.gyro'! Add a file backend with the name 'gyro-state' to copy state files to/from.");
        }

        Path rootDir = GyroCore.getRootDirectory();

        if (rootDir == null) {
            throw new GyroException(
                "Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.");
        }

        LocalFileBackend localStateBackend = new LocalFileBackend(rootDir.resolve(".gyro/state"));
        RemoteStateBackend remoteStateBackend = new RemoteStateBackend(remoteBackend, localStateBackend);
        boolean copiedFiles;

        if (getToLocal()) {
            GyroCore.ui().write("\n@|bold,white Looking for remote state files...|@\n\n");
            copiedFiles = remoteStateBackend.copyToLocal(false, true);
        } else {
            GyroCore.ui().write("\n@|bold,white Looking for local state files...|@\n\n");
            copiedFiles = remoteStateBackend.copyToRemote(false, true);
        }

        if (copiedFiles) {
            GyroCore.ui().write("\n@|bold,green OK|@\n");
        }
    }
}
