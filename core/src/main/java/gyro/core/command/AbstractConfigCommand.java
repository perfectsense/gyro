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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.LocalFileBackend;
import gyro.core.LockBackend;
import gyro.core.RemoteStateBackend;
import gyro.core.auth.Credentials;
import gyro.core.auth.CredentialsSettings;
import gyro.core.diff.ChangeProcessor;
import gyro.core.diff.ChangeSettings;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Option(names = "--skip-refresh", description = "Skip state refresh. Warning: Skipping refresh may result in incorrect changes. Use with caution.")
    public boolean skipRefresh;

    @Option(names = "--test", description = "Use for internal testing only. This flag will mock cloud provider API calls.")
    private boolean test;

    @Parameters(description = "Configuration files to process. Leave empty to process all files.")
    private List<String> files;

    protected abstract void doExecute(RootScope current, RootScope pending, State state) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        Path rootDir = GyroCore.getRootDirectory();

        if (rootDir == null) {
            throw new GyroException(
                "Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.");
        }

        Set<String> loadFiles;

        if (files == null) {
            loadFiles = null;

        } else {
            Map<Boolean, Set<String>> p = files.stream()
                .map(f -> f.endsWith(".gyro") ? f : f + ".gyro")
                .map(f -> rootDir.relativize(Paths.get("").toAbsolutePath().resolve(f)).normalize().toString())
                .collect(Collectors.partitioningBy(
                    f -> Files.exists(rootDir.resolve(f)),
                    Collectors.toCollection(LinkedHashSet::new)));

            Set<String> nonexistent = p.get(Boolean.FALSE);

            if (nonexistent.isEmpty()) {
                loadFiles = p.get(Boolean.TRUE);

            } else {
                throw new GyroException(String.format(
                    "Files not found! %s",
                    nonexistent.stream()
                        .map(f -> String.format("@|bold %s|@", f))
                        .collect(Collectors.joining(", "))));
            }
        }

        LocalFileBackend localTempBackend = new LocalFileBackend(rootDir.resolve(".gyro/.temp-state"));

        LockBackend lockBackend = GyroCore.getLockBackend();

        if (lockBackend != null) {
            lockBackend.setLocalTempBackend(localTempBackend);
            String lockId = lockBackend.readTempLockFile();

            if (lockId != null) {
                lockBackend.deleteTempLockFile();

                try {
                    lockBackend.unlock(lockId);
                } catch (Exception ex) {
                    // Ignore - the temp lock ID file is out of sync with current lock
                }
            }

            lockBackend.setLockId(UUID.randomUUID().toString());
            lockBackend.lock();
            lockBackend.updateLockInfo(String.format(
                "Locked by '%s' running '%s' at '%s'.",
                System.getProperty("user.name"),
                String.join(" ", getUnparsedArguments()),
                new SimpleDateFormat("HH:mm:ss zzz, MMM-dd").format(new Date())));
            lockBackend.writeTempLockFile();
        }

        try {
            RemoteStateBackend remoteStateBackend = Optional.ofNullable(GyroCore.getStateBackend("default"))
                .map(sb -> new RemoteStateBackend(sb, localTempBackend))
                .orElse(null);

            if (remoteStateBackend != null && !remoteStateBackend.isLocalBackendEmpty()) {
                if (!GyroCore.ui().readBoolean(
                    Boolean.FALSE,
                    "\n@|bold,red Temporary state files were detected, indicating a past failure pushing to a remote backend.\nWould you like to attempt to push the files to your remote backend?|@")) {
                    remoteStateBackend.deleteLocalBackend();
                } else {
                    remoteStateBackend.copyToRemote(true, true);
                }
            }

            RootScope current = new RootScope(
                "../../" + GyroCore.INIT_FILE,
                new LocalFileBackend(rootDir.resolve(".gyro/state")),
                remoteStateBackend,
                null,
                loadFiles);

            current.evaluate();

            RootScope pending = new RootScope(
                GyroCore.INIT_FILE,
                new LocalFileBackend(rootDir),
                current,
                loadFiles);

            if (!test) {
                current.getSettings(CredentialsSettings.class)
                    .getCredentialsByName()
                    .values()
                    .forEach(Credentials::refresh);

                if (!skipRefresh) {
                    refreshResources(current);
                }
            }
            GyroCore.ui().setAuditPending(true);

            pending.evaluate();
            pending.validate();

            doExecute(current, pending, new State(current, pending, test));
        } finally {
            if (lockBackend != null) {
                if (!lockBackend.stayLocked()) {
                    lockBackend.unlock();
                    lockBackend.deleteTempLockFile();
                }
            }
        }
    }

    private void refreshResources(RootScope scope) {
        ScheduledExecutorService messageService = Executors.newSingleThreadScheduledExecutor();
        GyroUI ui = GyroCore.ui();
        AtomicInteger started = new AtomicInteger();
        AtomicInteger done = new AtomicInteger();

        messageService.scheduleAtFixedRate(() -> {
            ui.replace("@|magenta ⟳ Refreshing resources:|@ %s started, %s done", started.get(), done.get());
        }, 0, 100, TimeUnit.MILLISECONDS);

        ExecutorService refreshService = Executors.newWorkStealingPool(25);
        List<Refresh> refreshes = new ArrayList<>();

        for (FileScope fileScope : scope.getFileScopes()) {
            for (Object value : fileScope.values()) {
                if (!(value instanceof Resource)) {
                    continue;
                }

                Resource resource = (Resource) value;

                List<ChangeProcessor> processors = new ArrayList<>();
                for (Scope s = DiffableInternals.getScope(resource); s != null; s = s.getParent()) {
                    processors.addAll(0, s.getSettings(ChangeSettings.class).getProcessors());
                }

                refreshes.add(new Refresh(resource, ui, refreshService.submit(() -> {
                    GyroCore.pushUi(ui);

                    started.incrementAndGet();

                    for (ChangeProcessor processor : processors) {
                        processor.beforeRefresh(ui, resource);
                    }

                    boolean keep = resource.refresh();

                    for (ChangeProcessor processor : processors) {
                        processor.afterRefresh(ui, resource);
                    }

                    if (keep) {
                        DiffableInternals.getModifications(resource).forEach(m -> m.refresh(resource));
                    }

                    done.incrementAndGet();

                    if (keep) {
                        DiffableInternals.disconnect(resource, true);
                        DiffableInternals.update(resource);
                        return false;

                    } else {
                        return true;
                    }
                })));
            }
        }

        refreshService.shutdown();

        for (Refresh refresh : refreshes) {
            Resource resource = refresh.resource;
            String typeName = DiffableType.getInstance(resource).getName();
            String name = DiffableInternals.getName(resource);

            try {
                if (refresh.future.get()) {
                    ui.replace("@|magenta - Removing from state:|@ %s %s\n", typeName, name);
                    scope.getFileScopes().forEach(s -> s.remove(resource.primaryKey()));
                }

            } catch (ExecutionException error) {
                refreshService.shutdownNow();
                messageService.shutdown();

                ui.write("\n");

                throw new GyroException(
                    String.format("Can't refresh @|bold %s %s|@ resource!", typeName, name),
                    error.getCause());

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        messageService.shutdown();
        ui.replace("@|magenta ⟳ Refreshed resources:|@ %s\n", refreshes.size());
    }

    private static class Refresh {

        public final Resource resource;
        public final Future<Boolean> future;
        public final GyroUI ui;

        public Refresh(Resource resource, GyroUI ui, Future<Boolean> future) {
            this.resource = resource;
            this.ui = ui;
            this.future = future;
        }

    }

}
