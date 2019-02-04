package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalFileBackend;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.RootScope;
import io.airlift.airline.Arguments;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    private BeamCore core;

    protected abstract void doExecute(FileScope current, FileScope pending) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        core = new BeamCore();

        RootScope pending;
        RootScope current;

        try {
            String pendingFile = arguments().get(0);
            pending = new RootScope(pendingFile);

            new LocalFileBackend().load(pending);

            String currentFile = pending.getFile() + ".state";
            Path currentFilePath = Paths.get(currentFile);
            current = new RootScope(currentFile);

            if (Files.exists(currentFilePath) && !Files.isDirectory(currentFilePath)) {
                pending.getBackend().load(current);
            }

        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        for (Map.Entry<String, Credentials> entry : pending.getCredentialsMap().entrySet()) {
            Credentials c = entry.getValue();

            c.findCredentials(true);
            current.getCredentialsMap().put(entry.getKey(), c);
        }

        doExecute(current, pending);
    }

    public List<String> arguments() {
        return arguments;
    }

    public BeamCore core() {
        return core;
    }
}
