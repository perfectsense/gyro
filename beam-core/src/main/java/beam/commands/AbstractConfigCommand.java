package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalFileBackend;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.FileBackend;
import beam.lang.Resource;
import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.RootScope;
import com.psddev.dari.util.StringUtils;
import io.airlift.airline.Arguments;
import io.airlift.airline.Option;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Option(name = { "--skip-refresh" })
    public boolean skipRefresh;

    @Arguments
    private List<String> arguments;

    private BeamCore core;

    public List<String> arguments() {
        return arguments;
    }

    public BeamCore core() {
        return core;
    }

    protected abstract void doExecute(RootScope current, RootScope pending) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        core = new BeamCore();

        FileBackend backend = new LocalFileBackend();

        String file = StringUtils.ensureEnd(
                StringUtils.removeEnd(arguments().get(0), ".bcl"),
                ".bcl.state");

        RootScope current = new RootScope(file);
        RootScope pending = new RootScope(current);

        try {
            backend.load(current);

        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        refreshCredentials(current);

        if (!skipRefresh) {
            refreshResources(current);
            BeamCore.ui().write("\n");
        }

        try {
            backend.load(pending);

        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        doExecute(current, pending);
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

}
