package gyro.cli;

import gyro.core.LocalFileBackend;
import gyro.core.command.AbstractCommand;
import gyro.core.command.GyroCommand;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.resource.RootScope;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gyro.core.resource.Scope;
import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.LoggerFactory;

import java.io.File;
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
        reflections = new Reflections(new org.reflections.util.ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage("gyro")));
    }

    public static void main(String[] arguments) throws Exception {
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

                init.load();

            } else {
                init = null;
            }

            gyro.init(Arrays.asList(arguments), init);
            gyro.run();

        } catch (Throwable error) {
            if (error instanceof GyroException) {
                GyroCore.ui().writeError(error.getCause(), "\n@|red Error: %s|@\n", error.getMessage());

            } else {
                GyroCore.ui().writeError(error, "\n@|red Unexpected error: %s|@\n", error.getMessage());
            }
        } finally {
            GyroCore.popUi();
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
        return reflections;
    }

}
