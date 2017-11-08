package beam;

import java.io.PrintWriter;

/**
 * CLI command.
 */
public interface BeamCommand {

    /**
     * @param out Can't be {@code null}.
     */
    public void execute() throws Exception;
}
