package gyro.cli;

import gyro.commands.AbstractCommand;
import gyro.commands.BeamCommand;
import gyro.commands.CliBeamUI;
import gyro.core.BeamCore;
import gyro.core.BeamException;
import gyro.core.LocalFileBackend;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.plugins.PluginLoader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Gyro {

    private Cli<Object> cli;
    private List<String> arguments;
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
        BeamCore.pushUi(new CliBeamUI());

        try {
            loadCommands(gyro);
            gyro.init(Arrays.asList(arguments));
            gyro.run();

        } catch (Throwable error) {
            if (error instanceof BeamException) {
                BeamCore.ui().writeError(error.getCause(), "\n@|red Error: %s|@\n", error.getMessage());

            } else {
                BeamCore.ui().writeError(error, "\n@|red Unexpected error! Stack trace follows:|@\n");
            }
        } finally {
            BeamCore.popUi();
        }
    }

    public void init(List<String> arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        commands().add(Help.class);

        for (Class<?> c : getReflections().getSubTypesOf(BeamCommand.class)) {
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
    }

    public Set<Class<?>> commands() {
        return commands;
    }

    public void run() throws Exception {
        Object command = cli.parse(arguments);

        if (command instanceof Runnable) {
            ((Runnable) command).run();

        } else if (command instanceof AbstractCommand) {
            ((AbstractCommand) command).execute();
        } else {
            throw new IllegalStateException(String.format(
                "[%s] must be an instance of [%s] or [%s]!",
                command.getClass().getName(),
                Runnable.class.getName(),
                BeamCommand.class.getName()));
        }
    }

    public static void loadCommands(Gyro gyro) throws Exception {
        // Load GYRO_ROOT/.gyro/plugins.gyro
        Path commandPluginPath = BeamCore.findCommandPluginPath();
        if (commandPluginPath != null) {
            File plugins = commandPluginPath.toFile();
            if (plugins.exists() && plugins.isFile()) {
                RootScope pluginConfig = new RootScope(plugins.toString());

                new LocalFileBackend().load(pluginConfig);

                for (PluginLoader loader : pluginConfig.getFileScope().getPluginLoaders()) {
                    for (Class<?> c : loader.classes()) {
                        if (BeamCommand.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                            gyro.commands().add(c);
                        }
                    }
                }
            }
        }
    }

    public static Reflections getReflections() {
        return reflections;
    }

    public static String getBeamUserHome() {
        String userHome = System.getenv("BEAM_USER_HOME");
        if (ObjectUtils.isBlank(userHome)) {
            userHome = System.getProperty("user.home");
        }

        return userHome;
    }

}
