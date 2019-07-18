package gyro.cli;

import gyro.core.Abort;
import gyro.core.LocalFileBackend;
import gyro.core.command.AbstractCommand;
import gyro.core.command.GyroCommand;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.RootScope;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.SyntaxError;
import gyro.lang.SyntaxErrorException;
import gyro.util.Bug;
import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Gyro {

    private Cli<Object> cli;
    private List<String> arguments;
    private Scope init;
    private Set<Class<?>> commands = new HashSet<Class<?>>();

    public static Reflections reflections;

    static {
        Reflections.log = null;
    }

    public static void main(String[] arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        Gyro gyro = new Gyro();
        GyroCore.pushUi(new CliGyroUI());

        try {
            Path rootDir = GyroCore.getRootDirectory();
            RootScope init;

            if (rootDir != null) {
                init = new RootScope(
                    GyroCore.INIT_FILE,
                    new LocalFileBackend(GyroCore.getRootDirectory()),
                    null,
                    Collections.emptySet());

                init.evaluate();

                if (init.values().stream().anyMatch(Resource.class::isInstance)) {
                    throw new GyroException(String.format("Resources are not allowed in '%s'%n", GyroCore.INIT_FILE));
                }

            } else {
                init = null;
            }

            gyro.init(Arrays.asList(arguments), init);
            gyro.run();

        } catch (Abort error) {
            GyroCore.ui().write("\n@|red Aborted!|@\n\n");

        } catch (Throwable error) {
            GyroCore.ui().write("\n");
            writeError(error);
            GyroCore.ui().write("\n");

        } finally {
            GyroCore.popUi();
        }
    }

    private static void writeError(Throwable error) {
        if (error instanceof Defer || error instanceof GyroException) {
            GyroCore.ui().write("@|red Error:|@ %s\n", error.getMessage());

            Locatable locatable = error instanceof Defer
                ? ((Defer) error).getNode()
                : ((GyroException) error).getLocatable();

            if (locatable != null) {
                GyroCore.ui().write("\nIn @|bold %s|@ %s:\n", locatable.getFile(), locatable.toLocation());
                GyroCore.ui().write("%s", locatable.toCodeSnippet());
            }

            Throwable cause = error.getCause();

            if (cause != null) {
                GyroCore.ui().write("\n@|red Caused by:|@ ");
                writeError(cause);
            }

        } else if (error instanceof SyntaxErrorException) {
            SyntaxErrorException s = (SyntaxErrorException) error;
            List<SyntaxError> syntaxErrors = s.getSyntaxErrors();

            GyroCore.ui().write("@|red %d syntax errors in %s!|@\n", syntaxErrors.size(), s.getFile());

            for (SyntaxError syntaxError : syntaxErrors) {
                GyroCore.ui().write("\n%s %s:\n", syntaxError.getMessage(), syntaxError.toLocation());
                GyroCore.ui().write("%s", syntaxError.toCodeSnippet());
            }

        } else {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            error.printStackTrace(pw);

            if (error instanceof Bug) {
                GyroCore.ui().write("@|red This should've never happened. Please report this as a bug with the following stack trace:|@ %s\n", sw.toString());

            } else {
                GyroCore.ui().write("@|red Unexpected error:|@ %s\n", sw.toString());
            }
        }
    }

    public void init(List<String> arguments, Scope init) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        commands().add(Help.class);

        for (Class<?> c : getReflections().getSubTypesOf(GyroCommand.class)) {
            if (c.isAnnotationPresent(Command.class)) {
                commands().add(c);
            }
        }

        String appName = "gyro";
        if (System.getProperty("gyro.app") != null) {
            File appFile = new File(System.getProperty("gyro.app"));
            if (appFile.exists()) {
                appName = appFile.getName();
            }
        }

        Cli.CliBuilder<Object> builder = Cli.<Object>builder(appName)
            .withDescription("Gyro.")
            .withDefaultCommand(Help.class)
            .withCommands(commands());

        this.cli = builder.build();
        this.arguments = arguments;
        this.init = init;
    }

    public Set<Class<?>> commands() {
        return commands;
    }

    public void run() throws Exception {
        Object command = cli.parse(arguments);

        if (command instanceof Runnable) {
            ((Runnable) command).run();

        } else if (command instanceof AbstractCommand) {
            ((AbstractCommand) command).setInit(init);
            ((AbstractCommand) command).execute();

        } else {
            throw new IllegalStateException(String.format(
                "[%s] must be an instance of [%s] or [%s]!",
                command.getClass().getName(),
                Runnable.class.getName(),
                GyroCommand.class.getName()));
        }
    }

    public static Reflections getReflections() {
        if (reflections == null) {
            reflections = new Reflections(new org.reflections.util.ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("gyro")));
        }

        return reflections;
    }

}
