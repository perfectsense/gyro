package gyro.core.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.core.auth.Credentials;
import gyro.core.auth.CredentialsSettings;
import gyro.core.resource.DiffableType;
import gyro.core.resource.FileScope;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.State;
import io.airlift.airline.Arguments;
import io.airlift.airline.Option;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Option(name = { "--skip-refresh" })
    public boolean skipRefresh;

    @Option(name = { "--test" })
    private boolean test;

    @Arguments
    private List<String> files;

    private GyroCore core;

    public GyroCore core() {
        return core;
    }

    protected abstract void doExecute(RootScope current, RootScope pending, State state) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        Path rootDir = GyroCore.getRootDirectory();

        if (rootDir == null) {
            throw new GyroException("Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.");
        }

        Set<String> loadFiles;
        Set<String> diffFiles;

        if (getInit().getSettings(HighlanderSettings.class).isHighlander()) {
            if (files != null) {
                if (files.size() == 1) {
                    loadFiles = Collections.singleton(resolve(rootDir, files.get(0)));
                    diffFiles = null;

                } else {
                    throw new GyroException("Can't specify more than one file in highlander mode!");
                }

            } else {
                throw new GyroException("Must specify a file in highlander mode!");
            }

        } else if (files != null) {
            loadFiles = null;
            diffFiles = files.stream()
                .map(f -> resolve(rootDir, f))
                .collect(Collectors.toSet());

        } else {
            loadFiles = null;
            diffFiles = null;
        }

        core = new GyroCore();

        RootScope current = new RootScope(
            "../../" + GyroCore.INIT_FILE,
            new LocalFileBackend(rootDir.resolve(".gyro/state")),
            null,
            loadFiles);

        current.load();

        RootScope pending = new RootScope(
            GyroCore.INIT_FILE,
            new LocalFileBackend(rootDir),
            current,
            loadFiles);

        pending.load();

        if (!test) {
            current.getSettings(CredentialsSettings.class)
                .getCredentialsByName()
                .values()
                .forEach(Credentials::refresh);

            if (!skipRefresh) {
                refreshResources(current);
                GyroCore.ui().write("\n");
            }
        }

        doExecute(current, pending, new State(current, pending, test, diffFiles));
    }

    private String resolve(Path rootDir, String file) {
        file = file.endsWith(".gyro") ? file : file + ".gyro";
        file = rootDir.relativize(Paths.get("").toAbsolutePath().resolve(file)).normalize().toString();

        if (Files.exists(rootDir.resolve(file))) {
            return file;

        } else {
            throw new GyroException(String.format("File not found! %s", file));
        }
    }

    private void refreshResources(RootScope scope) {
        ExecutorService service = Executors.newCachedThreadPool();
        List<Refresh> refreshes = new ArrayList<>();

        for (FileScope fileScope : scope.getFileScopes()) {
            for (Object value : fileScope.values()) {
                if (!(value instanceof Resource)) {
                    continue;
                }

                Resource resource = (Resource) value;

                refreshes.add(new Refresh(resource, service.submit(() -> {
                    if (resource.refresh()) {
                        resource.updateInternals();
                        return false;

                    } else {
                        return true;
                    }
                })));
            }
        }

        GyroCore.ui().write("@|magenta ⟳ Refreshing resources:|@ %s\n", refreshes.size());
        service.shutdown();

        for (Refresh refresh : refreshes) {
            Resource resource = refresh.resource;
            String typeName = DiffableType.getInstance(resource.getClass()).getName();
            String name = resource.name();

            try {
                if (refresh.future.get()) {
                    GyroCore.ui().write("@|magenta - Removing from state:|@ %s %s\n", typeName, name);
                    scope.getFileScopes().forEach(s -> s.remove(resource.primaryKey()));
                }

            } catch (ExecutionException error) {
                throw new GyroException(
                    String.format("Can't refresh @|bold %s %s|@ resource!", typeName, name),
                    error.getCause());

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static class Refresh {

        public final Resource resource;
        public final Future<Boolean> future;

        public Refresh(Resource resource, Future<Boolean> future) {
            this.resource = resource;
            this.future = future;
        }

    }

}
