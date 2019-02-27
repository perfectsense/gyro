package gyro.commands;

import gyro.core.BeamCore;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.slf4j.LoggerFactory;

/**
 * Basic {@link BeamCommand} implementation that adds the global {@code -debug} option for more detailed logging.
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

    @Option(name = { "--verbose" })
    private boolean verbose;

    protected abstract void doExecute() throws Exception;

    @Override
    public void execute() throws Exception {
        BeamCore.ui().setVerbose(verbose);

        if (debug || "debug".equalsIgnoreCase(System.getenv("BEAM_LOG"))) {
            System.getProperties().setProperty("org.openstack4j.core.transport.internal.HttpLoggingFilter", "true");

            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
        } else {
            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        }

        doExecute();
    }

    public boolean isDebug() {
        return debug;
    }

}
