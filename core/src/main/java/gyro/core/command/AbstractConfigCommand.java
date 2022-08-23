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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import gyro.core.resource.RefreshException;
import gyro.core.resource.Resource;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import org.apache.commons.lang.time.StopWatch;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Option(names = "--skip-refresh", description = "Skip state refresh. Warning: Skipping refresh may result in incorrect changes. Use with caution.")
    public boolean skipRefresh;

    @Option(names = "--local-refresh", description = "Only refresh resources found in the current working directory.")
    public boolean localRefresh;

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
        GyroUI ui = GyroCore.ui();

        ExecutorService refreshService = Executors.newWorkStealingPool();
        List<Refresh> refreshes = new ArrayList<>();
        Map<DiffableType, List<Resource>> refreshQueues = new HashMap<>();

        // Group Resources by type.
        for (FileScope fileScope : scope.getFileScopes()) {
            String currentDirectory = System.getProperty("user.dir");
            String currentFile = GyroCore.getRootDirectory().resolve(fileScope.getFile()).getParent().toString();

            if (!currentDirectory.equals(currentFile) && localRefresh) {
                continue;
            }

            for (Object value : fileScope.values()) {
                if (!(value instanceof Resource)) {
                    continue;
                }

                Resource resource = (Resource) value;

                List<ChangeProcessor> processors = new ArrayList<>();
                for (Scope s = DiffableInternals.getScope(resource); s != null; s = s.getParent()) {
                    processors.addAll(0, s.getSettings(ChangeSettings.class).getProcessors());
                }

                List<Resource> refreshQueue =
                    refreshQueues.computeIfAbsent(DiffableType.getInstance(resource), m -> new ArrayList<>());
                refreshQueue.add(resource);
            }
        }

        // Refresh each type as a group.
        for (DiffableType type : refreshQueues.keySet()) {
            List<Resource> refreshQueue = refreshQueues.get(type);

            refreshes.add(new Refresh(refreshQueue, ui, refreshService.submit(() -> {
                GyroCore.pushUi(ui);

                if (refreshQueue.isEmpty()) {
                    return null;
                }

                Resource peek = refreshQueue.get(0);
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                // Run beforeRefresh processors
                for (Resource resource : refreshQueue) {
                    List<ChangeProcessor> processors = new ArrayList<>();
                    for (Scope s = DiffableInternals.getScope(resource); s != null; s = s.getParent()) {
                        processors.addAll(0, s.getSettings(ChangeSettings.class).getProcessors());
                    }

                    for (ChangeProcessor processor : processors) {
                        processor.beforeRefresh(ui, resource);
                    }
                }

                Map<? extends Resource, Boolean> refreshResults = peek.batchRefresh(refreshQueue);

                // Run afterRefresh processors
                for (Resource resource : refreshQueue) {
                    List<ChangeProcessor> processors = new ArrayList<>();
                    for (Scope s = DiffableInternals.getScope(resource); s != null; s = s.getParent()) {
                        processors.addAll(0, s.getSettings(ChangeSettings.class).getProcessors());
                    }

                    for (ChangeProcessor processor : processors) {
                        processor.afterRefresh(ui, resource);
                    }

                    if (refreshResults.containsKey(resource) && refreshResults.get(resource)) {
                        DiffableInternals.getModifications(resource).forEach(m -> m.refresh(resource));
                        DiffableInternals.disconnect(resource, true);
                        DiffableInternals.update(resource);
                    }
                }

                stopWatch.stop();
                Duration duration = Duration.ofMillis(stopWatch.getTime());

                String time = "";
                if (duration.getSeconds() <= 10) {
                    time = String.format("%dms", duration.toMillis());
                } else {
                    time = String.format("%dm%ds", duration.toMinutes(), (duration.getSeconds() - (duration.toMinutes() * 60)));
                }

                ui.write("Refreshing @|magenta,bold %s|@: @|green %s|@ %s refreshed in @|green %s|@ elapsed\n",
                    DiffableType.getInstance(peek).getName(),
                    refreshResults.size(),
                    refreshResults.size() == 1 ? "resource" : "resources",
                    time);

                for (Resource resource : refreshResults.keySet()) {
                    boolean refreshed = refreshResults.get(resource);

                    String typeName = DiffableType.getInstance(resource).getName();
                    String name = DiffableInternals.getName(resource);

                    if (!refreshed) {
                        ui.replace("@|magenta - Removing from state:|@ %s %s\n", typeName, name);
                        scope.getFileScopes().forEach(s -> s.remove(resource.primaryKey()));
                    }
                }

                return null;
            })));

            for (Refresh refresh : refreshes) {
                try {
                    refresh.future.get();
                } catch (InterruptedException erorr) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause();
                    if (cause instanceof RefreshException) {
                        ui.write("\n");

                        RefreshException refreshException = (RefreshException) cause;

                        Resource resource = refreshException.getResource();
                        String typeName = DiffableType.getInstance(resource).getName();
                        String name = DiffableInternals.getName(resource);

                        throw new GyroException(
                            String.format("Can't refresh @|bold %s %s|@ resource!", typeName, name),
                            error.getCause());
                    } else {

                        throw new GyroException(
                            String.format("Can't refresh @|bold %s |@ resource group!", refresh.typeName()),
                            error.getCause());
                    }
                }
            }
        }

        refreshService.shutdown();
    }

    private static class Refresh {

        public final List<Resource> resources;
        public final Future<?> future;
        public final GyroUI ui;

        public Refresh(List<Resource> resources, GyroUI ui, Future<?> future) {
            this.resources = resources;
            this.ui = ui;
            this.future = future;
        }

        public String typeName() {
            Resource resource = resources.get(0);
            return DiffableType.getInstance(resource).getName();
        }
    }

}
