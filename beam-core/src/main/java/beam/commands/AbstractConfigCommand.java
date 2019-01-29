package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.StateBackend;
import beam.lang.ast.Scope;
import io.airlift.airline.Arguments;

import java.util.List;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    private BeamCore core;

    protected abstract void doExecute(Scope pending, Scope state) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        String configPath = arguments().get(0);
        core = new BeamCore();
        Scope pendingScope;
        Scope stateScope;
        try {
            pendingScope = core.parseScope(configPath);
            StateBackend stateBackend = pendingScope.getStateBackend();
            stateScope = stateBackend.load(pendingScope);
        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        for (Credentials credentials : pendingScope.getCredentials()) {
            credentials.findCredentials(true);
        }

        doExecute(pendingScope, stateScope);
    }

    public List<String> arguments() {
        return arguments;
    }

    public BeamCore core() {
        return core;
    }
}
