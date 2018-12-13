package beam.cli;

import beam.core.BeamCore;
import beam.core.BeamException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Beam {

    private final Cli<Object> cli;
    private final List<String> arguments;

    public static final Reflections reflections;

    static {
        Reflections.log = null;
        reflections = new Reflections(new org.reflections.util.ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("beam")));
    }

    public static void main(String[] arguments) throws Exception {
        BeamCore.pushUi(new CLIBeamUI());

        try {
            Beam beam = new Beam(Arrays.asList(arguments));
            beam.run();

        } finally {
            BeamCore.popUi();
        }
    }

    public Beam(List<String> arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        Set<Class<?>> commands = new HashSet<Class<?>>();

        commands.add(Help.class);

        for (Class<?> c : getReflections().getSubTypesOf(BeamCommand.class)) {
            if (c.isAnnotationPresent(Command.class)) {
                commands.add(c);
            }
        }

        String appName = "beam";
        if (System.getProperty("beam.app") != null) {
            File appFile = new File(System.getProperty("beam.app"));
            if (appFile.exists()) {
                appName = appFile.getName();
            }
        }

        Cli.CliBuilder<Object> builder = Cli.<Object>builder(appName).
                withDescription("Beam.").
                withDefaultCommand(Help.class).
                withCommands(commands);

        Set<Class<?>> serviceCommands = new HashSet<Class<?>>();

        builder.withGroup("service").
                withDefaultCommand(Help.class).
                withDescription("Beam service (client and server) commands.").
                withCommands(serviceCommands);

        this.cli = builder.build();
        this.arguments = arguments;
    }

    public void run() throws IOException {
        try {
            Object command = cli.parse(arguments);

            if (command instanceof Runnable) {
                ((Runnable) command).run();

            } else if (command instanceof AbstractCommand) {
                ((AbstractCommand) command).doExecute();
            } else {
                throw new IllegalStateException(String.format(
                        "[%s] must be an instance of [%s] or [%s]!",
                        command.getClass().getName(),
                        Runnable.class.getName(),
                        BeamCommand.class.getName()));
            }

        } catch (Throwable error) {
            if (error instanceof BeamException) {
                BeamCore.ui().writeError(error.getCause(), "\n@|red Error: %s|@", error.getMessage());

            } else {
                BeamCore.ui().writeError(error, "\n@|red Unexpected error! Stack trace follows:|@");
            }
        }
    }

    public static Reflections getReflections() {
        return reflections;
    }

    private static List<String> parseAlias(String alias) {
        Pattern pattern = Pattern.compile("([^\"'\\s]+)|\"([^\"]*)\"|'([^']*)'");
        Matcher matcher = pattern.matcher(alias);

        List<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.group(2) != null) {
                arguments.add(matcher.group(2));
            } else if (matcher.group(3) != null) {
                arguments.add(matcher.group(3));
            } else {
                arguments.add(matcher.group());
            }
        }

        return arguments;
    }
}
