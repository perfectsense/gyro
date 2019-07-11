package gyro.core.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.core.auth.Credentials;
import gyro.core.auth.CredentialsSettings;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.FileScope;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.State;
import gyro.lang.GyroLanguageException;
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

        try {
            current.load();

        } catch (GyroLanguageException ex) {
            throw new GyroException(ex.getMessage());
        }

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

        RootScope pending = new RootScope(
            GyroCore.INIT_FILE,
            new LocalFileBackend(rootDir),
            current,
            loadFiles);

        try {
            pending.load();

        } catch (GyroLanguageException ex) {
            throw new GyroException(ex.getMessage());
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
        for (FileScope fileScope : scope.getFileScopes()) {
            for (Iterator<Map.Entry<String, Object>> i = fileScope.entrySet().iterator(); i.hasNext(); ) {
                Object value = i.next().getValue();

                if (value instanceof Resource) {
                    Resource resource = (Resource) value;

                    GyroCore.ui().write(
                        "@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        DiffableType.getInstance(resource.getClass()).getName(),
                        resource.name());

                    if (resource.refresh()) {
                        DiffableInternals.update(resource);

                    } else {
                        i.remove();
                    }

                    GyroCore.ui().write("\n");
                }
            }
        }
    }

}
