package gyro.commands;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.lang.GyroLanguageException;
import gyro.lang.Credentials;
import gyro.lang.FileBackend;
import gyro.core.resource.Resource;
import gyro.lang.ast.scope.FileScope;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.State;
import com.psddev.dari.util.StringUtils;
import io.airlift.airline.Arguments;
import io.airlift.airline.Option;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Option(name = { "--skip-refresh" })
    public boolean skipRefresh;

    @Option(name = { "--test" })
    private boolean test;

    @Arguments
    private List<String> arguments;

    private GyroCore core;

    public List<String> arguments() {
        return arguments;
    }

    public GyroCore core() {
        return core;
    }

    protected abstract void doExecute(RootScope current, RootScope pending, State state) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new GyroException("Gyro configuration file required.");
        }

        core = new GyroCore();

        FileBackend backend = new LocalFileBackend();

        String file = StringUtils.ensureEnd(arguments().get(0), ".gyro");
        if (!Files.exists(Paths.get(file))) {
            throw new GyroException(String.format("File '%s' not found.", file));
        }

        file += ".state";

        RootScope current = new RootScope(file);
        RootScope pending = new RootScope(current);

        try {
            backend.load(current);

        } catch (GyroLanguageException ex) {
            throw new GyroException(ex.getMessage());
        }

        if (!test) {
            refreshCredentials(current);

            if (!skipRefresh) {
                refreshResources(current);
                GyroCore.ui().write("\n");
            }
        }

        try {
            backend.load(pending);

        } catch (GyroLanguageException ex) {
            throw new GyroException(ex.getMessage());
        }

        doExecute(current, pending, new State(pending, test));
    }

    private void refreshCredentials(FileScope scope) {
        scope.getImports().forEach(this::refreshCredentials);

        scope.values()
                .stream()
                .filter(Credentials.class::isInstance)
                .map(Credentials.class::cast)
                .forEach(c -> c.findCredentials(true));
    }

    private void refreshResources(FileScope scope) {
        scope.getImports().forEach(this::refreshResources);

        for (Iterator<Map.Entry<String, Object>> i = scope.entrySet().iterator(); i.hasNext();) {
            Object value = i.next().getValue();

            if (value instanceof Resource && !(value instanceof Credentials)) {
                Resource resource = (Resource) value;

                GyroCore.ui().write(
                        "@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        resource.resourceType(),
                        resource.resourceIdentifier());

                if (!resource.refresh()) {
                    i.remove();
                }

                GyroCore.ui().write("\n");
            }
        }
    }

}
