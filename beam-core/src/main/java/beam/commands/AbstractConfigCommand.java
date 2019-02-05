package beam.commands;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalFileBackend;
import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.FileBackend;
import beam.lang.ast.scope.RootScope;
import com.psddev.dari.util.StringUtils;
import io.airlift.airline.Arguments;

import java.util.List;
import java.util.Map;

public abstract class AbstractConfigCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    private BeamCore core;

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
            backend.load(pending);

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
