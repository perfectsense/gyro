package beam.cli;

import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.io.PrintWriter;
import java.util.List;

import org.slf4j.LoggerFactory;

import beam.BeamCommand;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Basic {@link BeamCommand} implementation that adds the global
 * {@code -debug} option for more detailed logging.
 *
 * <p>Subclasses must override:</p>
 *
 * <ul>
 * <li>{@link #doExecute}</li>
 * </ul>
 */
public abstract class AbstractCommand implements BeamCommand {

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public boolean debug;

    private List<String> unparsedArgument;

    /**
     * @param out Can't be {@code null}.
     */
    protected abstract void doExecute() throws Exception;

    public final void execute() throws Exception {
        if (debug || "debug".equalsIgnoreCase(System.getenv("BEAM_LOG"))) {
            System.getProperties().setProperty("org.openstack4j.core.transport.internal.HttpLoggingFilter", "true");

            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
        } else {
            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

            System.setProperty("org.apache.commons.logging.Log",
                    "org.apache.commons.logging.impl.NoOpLog");
        }

        doExecute();
    }

    public List<String> getUnparsedArgument() {
        return unparsedArgument;
    }

    public void setUnparsedArgument(List<String> unparsedArgument) {
        this.unparsedArgument = unparsedArgument;
    }
}
