package gyro.commands;

import gyro.core.BeamCore;
import gyro.core.BeamException;
import gyro.core.LocalFileBackend;
import gyro.lang.BeamLanguageException;
import gyro.lang.Credentials;
import gyro.lang.FileBackend;
import gyro.lang.Resource;
import gyro.lang.ast.scope.FileScope;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.State;
import com.psddev.dari.util.StringUtils;
import io.airlift.airline.Arguments;
import io.airlift.airline.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Option(name = { "--skip-refresh" })
    public boolean skipRefresh;

    @Option(name = { "--test" })
    private boolean test;

    @Arguments
    private List<String> arguments;

    private BeamCore core;

    public List<String> arguments() {
        return arguments;
    }

    public BeamCore core() {
        return core;
    }

    protected abstract void doExecute(RootScope current, RootScope pending, State state) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        core = new BeamCore();

        FileBackend backend = new LocalFileBackend();

        String file = StringUtils.ensureEnd(
                StringUtils.removeEnd(arguments().get(0), ".gyro"),
                ".gyro.state");

        RootScope current = new RootScope(file);
        RootScope pending = new RootScope(current);

        try {
            backend.load(pending);

        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        loadCurrent(pending, current);

        if (!test) {
            refreshCredentials(current);

            if (!skipRefresh) {
                refreshResources(current);
                BeamCore.ui().write("\n");
            }
        }

        pending = new RootScope(current);

        try {
            backend.load(pending);

        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        State state = new State(pending, test);
        doExecute(current, pending, state);
        state.cleanUp();
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

                BeamCore.ui().write(
                        "@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        resource.resourceType(),
                        resource.resourceIdentifier());

                if (!resource.refresh()) {
                    i.remove();
                }

                BeamCore.ui().write("\n");
            }
        }
    }

    private void loadCurrent(FileScope pending, FileScope current) throws Exception {
        pending.getBackend().load(current);
        Map<String, FileScope> pendingImports = pending.getImports().stream()
            .collect(Collectors.toMap(i -> getStateFile(i.getFile()), Function.identity()));

        for (FileScope stateImport : current.getImports()) {
            if (!pendingImports.keySet().contains(stateImport.getFile())) {
                stateImport.values().removeIf(r -> r instanceof Resource && !(r instanceof Credentials));
            } else {
                pendingImports.remove(stateImport.getFile());
            }
        }

        for (Map.Entry<String, FileScope> entry : pendingImports.entrySet()) {
            Path pendingDir = Paths.get(pending.getFile()).getParent() != null 
                ? Paths.get(pending.getFile()).getParent() : Paths.get(".");

            FileScope stateImport = new FileScope(current, pendingDir.relativize(Paths.get(entry.getKey())).toString());
            loadCurrent(entry.getValue(), stateImport);
            current.getImports().add(stateImport);
        }
    }

    private String getStateFile(String file) {
        if (file.endsWith(".gyro.state")) {
            return file;

        } else if (file.endsWith(".gyro")) {
            return file + ".state";

        } else {
            return file + ".gyro.state";
        }
    }
}
