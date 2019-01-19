package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.lang.BeamFile;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import io.airlift.airline.Arguments;

import java.util.List;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    private BeamCore core;

    protected abstract void doExecute(BeamFile beamFile) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        if (arguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        String configPath = arguments().get(0);
        core = new BeamCore();
        BeamFile pending;
        try {
            pending = core.parse(configPath);
        } catch (BeamLanguageException ex) {
            throw new BeamException(ex.getMessage());
        }

        for (Credentials credentials : pending.credentials()) {
            credentials.findCredentials(true);
        }

        doExecute(pending);
    }

    public List<String> arguments() {
        return arguments;
    }

    public BeamCore core() {
        return core;
    }
}
