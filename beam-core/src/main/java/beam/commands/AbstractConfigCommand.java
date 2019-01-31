package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalStateBackend;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.ast.scope.FileScope;
import io.airlift.airline.Arguments;

import java.util.List;

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

        String file = arguments().get(0);
        FileScope pending;
        FileScope current;

        try {
            pending = new LocalStateBackend().load(null, file);
            current = pending.getStateBackend().load(null, pending.getFile() + ".state");

        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        for (Credentials credentials : pending.getCredentials()) {
            credentials.findCredentials(true);
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
