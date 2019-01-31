package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.StateBackend;
import beam.lang.ast.scope.FileScope;
import io.airlift.airline.Arguments;

import java.util.List;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    private BeamCore core;

    protected abstract void doExecute(FileScope pending) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        String configPath = arguments().get(0);
        core = new BeamCore();
        FileScope pendingScope;
        FileScope stateScope;
        try {
            pendingScope = core.parse(configPath);
            StateBackend stateBackend = pendingScope.getFileScope().getStateBackend();
            stateScope = stateBackend.load(pendingScope);

            pendingScope.setState(stateScope);
        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        for (Credentials credentials : pendingScope.getCredentials()) {
            credentials.findCredentials(true);
        }

        doExecute(pendingScope);
    }

    public List<String> arguments() {
        return arguments;
    }

    public BeamCore core() {
        return core;
    }
}
