package gyro.core.command;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.lang.GyroLanguageException;
import gyro.core.Credentials;
import gyro.core.FileBackend;
import gyro.core.resource.Resource;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.State;
import io.airlift.airline.Arguments;
import io.airlift.airline.Option;

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
        core = new GyroCore();

        FileBackend backend = new LocalFileBackend();
        RootScope current = new RootScope();
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

    private void refreshCredentials(RootScope scope) {
        for (FileScope fileScope : scope.getFileScopes()) {
            fileScope.values()
                .stream()
                .filter(Credentials.class::isInstance)
                .map(Credentials.class::cast)
                .forEach(c -> c.findCredentials(true));
        }
    }

    private void refreshResources(RootScope scope) {
        for (FileScope fileScope : scope.getFileScopes()) {
            for (Iterator<Map.Entry<String, Object>> i = fileScope.entrySet().iterator(); i.hasNext(); ) {
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

}
