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

import java.util.Optional;

import gyro.core.GyroCore;
import gyro.core.GyroUI;
import gyro.core.RemoteStateBackend;
import gyro.core.diff.Diff;
import gyro.core.diff.Retry;
import gyro.core.scope.RootScope;
import gyro.core.scope.State;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "up", description = "Updates all resources to match the configuration.", mixinStandardHelpOptions = true)
public class UpCommand extends AbstractConfigCommand {

    @Option(names = "--skip-version-check", description = "Skips checking for the latest version.")
    public boolean skipVersionCheck;
    private boolean auditStarted;

    @Override
    public void doExecute(RootScope current, RootScope pending, State state) throws Exception {
        if (!skipVersionCheck) {
            VersionCommand.printUpdateVersion();
        }

        GyroUI ui = GyroCore.ui();

        ui.write("\n@|bold,white Looking for changes...\n\n|@");

        while (true) {
            Diff diff = new Diff(
                current.findSortedResourcesIn(current.getLoadFiles()),
                pending.findSortedResourcesIn(pending.getLoadFiles()));

            diff.diff();

            if (!diff.write(ui)) {
                ui.write("\n@|bold,green No changes.|@\n\n");
                break;
            }

            if (!ui.readBoolean(Boolean.FALSE, "\nAre you sure you want to change resources?")) {
                break;
            }

            if (!auditStarted) {
                startAuditors(ui);
                auditStarted = true;
            }

            ui.write("\n");

            try {
                diff.execute(ui, state);
                break;

            } catch (Retry error) {
                ui.write("\n@|bold,white Relooking for changes after workflow...\n\n|@");

                current = new RootScope(
                    current.getFile(),
                    current.getBackend(),
                    current.getRemoteStateBackend(),
                    null,
                    current.getLoadFiles());

                current.evaluate();

                pending = new RootScope(
                    pending.getFile(),
                    pending.getBackend(),
                    current,
                    pending.getLoadFiles());

                pending.evaluate();
                pending.validate();

                state = new State(current, pending, state.isTest());
            } catch (Exception ex) {
                ui.write("\n\n");
                pushToRemote(current, ui);

                throw ex;
            }
        }

        pushToRemote(current, ui);

        ui.finishAuditors(null, true);
    }

    private void pushToRemote(RootScope current, GyroUI ui) {
        RemoteStateBackend remoteStateBackend = current.getRemoteStateBackend();
        if (remoteStateBackend != null && !remoteStateBackend.isLocalBackendEmpty()) {
            ui.write(ui.isVerbose()
                ? "@|bold,white Pushing state files to remote backend...|@"
                : "\n@|bold,white Pushing state files to remote backend...|@");

            try {
                remoteStateBackend.copyToRemote(true, false);
                ui.write(ui.isVerbose() ? "\n@|bold,green OK|@\n\n" : "@|bold,green  OK|@\n");

            } catch (Exception ex) {
                ui.write("\n\n@|red Error pushing state files to remote: |@" + ex.getMessage()
                    + "\n\n@|red Run 'gyro up' again to retry.\n\n|@");

                Optional.ofNullable(GyroCore.getLockBackend()).ifPresent(l -> l.setStayLocked(true));
            }
        }
    }
}
