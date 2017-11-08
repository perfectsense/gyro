package beam;

import beam.cli.AbstractCommand;
import beam.cli.ProvisionCommand;
import beam.cli.RemoteCommand;
import beam.cli.ServerCommand;
import beam.cli.VersionCommand;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.airlift.command.Cli;
import io.airlift.command.Command;
import io.airlift.command.Help;
import org.fusesource.jansi.AnsiRenderWriter;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Beam {

    private final Cli<Object> cli;
    private final List<String> arguments;

    public static final Reflections reflections;

    static {
        Reflections.log = null;
        reflections = Reflections.collect();
    }

    public static void main(String[] arguments) throws Exception {
        List<String> beamArguments = new ArrayList<>();
        beamArguments.addAll(Arrays.asList(arguments));

        Map aliasMap = BeamConfig.get(Map.class, "alias", null);
        if (aliasMap != null) {
            String command = arguments[0];

            for (Object aliasKey : aliasMap.keySet()) {
                if (aliasKey.equals(command)) {
                    String alias = (String) aliasKey;
                    String value = (String) aliasMap.get(alias);
                    String[] nonAliasArgs = Arrays.copyOfRange(arguments, 1, arguments.length);

                    beamArguments = parseAlias(value);

                    // Replace any positional values with arguments from commandline.
                    Set<Integer> positionsUsed = new HashSet<Integer>();
                    for (int i = 0; i < beamArguments.size(); i++) {
                        String argument = beamArguments.get(i);

                        Pattern posPattern = Pattern.compile("\\$(\\d+)");
                        Matcher matcher = posPattern.matcher(argument);

                        while (matcher.find()) {
                            Integer pos = Integer.valueOf(matcher.group(1));

                            if (pos < nonAliasArgs.length) {
                                argument = argument.replaceAll("\\$" + pos, nonAliasArgs[pos - 1]);

                                beamArguments.set(i, argument);
                                positionsUsed.add(pos - 1);
                            }
                        }
                    }

                    for (Integer pos : positionsUsed) {
                        nonAliasArgs[pos] = null;
                    }

                    for (String argument : nonAliasArgs) {
                        if (argument != null) {
                            beamArguments.add(argument);
                        }
                    }

                    break;
                }
            }
        }

        Beam beam = new Beam(beamArguments);
        beam.run();
    }

    public Beam(List<String> arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        Set<Class<?>> commands = new HashSet<Class<?>>();

        commands.add(Help.class);
        commands.add(ProvisionCommand.class);
        commands.add(VersionCommand.class);

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

        serviceCommands.add(RemoteCommand.HostsfileCommand.class);
        serviceCommands.add(ServerCommand.class);

        builder.withGroup("service").
                withDefaultCommand(Help.class).
                withDescription("Beam service (client and server) commands.").
                withCommands(serviceCommands);

        this.cli = builder.build();
        this.arguments = arguments;
    }

    public void run() {
        try {
            Object command = cli.parse(arguments);

            if (command instanceof Runnable) {
                ((Runnable) command).run();

            } else if (command instanceof BeamCommand) {
                if (command instanceof AbstractCommand) {
                    ((AbstractCommand) command).setUnparsedArgument(new ArrayList(arguments));
                }

                ((BeamCommand) command).execute();
            } else {
                throw new IllegalStateException(String.format(
                        "[%s] must be an instance of [%s] or [%s]!",
                        command.getClass().getName(),
                        Runnable.class.getName(),
                        BeamCommand.class.getName()));
            }

        } catch (Throwable error) {
            Throwable cause = null;

            PrintWriter out = new AnsiRenderWriter(System.out, true);
            if (error instanceof BeamException) {
                out.write("@|red Error: " + error.getMessage() + "|@\n");
                out.flush();

                cause = error.getCause();

            } else {
                out.write("@|red Unexpected error! Stack trace follows:|@\n");
                out.flush();

                cause = error;
            }

            if (cause != null) {
                out.write(cause.getClass().getName());
                out.write(": ");
                cause.printStackTrace(out);
                out.flush();
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
