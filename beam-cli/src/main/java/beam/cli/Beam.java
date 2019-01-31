package beam.cli;

import beam.commands.AbstractCommand;
import beam.commands.BeamCommand;
import beam.commands.CliBeamUI;
import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalStateBackend;
import beam.lang.ast.scope.Scope;
import beam.lang.plugins.PluginLoader;
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
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Beam {

    private Cli<Object> cli;
    private List<String> arguments;
    private Set<Class<?>> commands = new HashSet<Class<?>>();

    public static Reflections reflections;

    static {
        Reflections.log = null;
        reflections = new Reflections(new org.reflections.util.ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage("beam")));
    }

    public static void main(String[] arguments) throws Exception {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        Beam beam = new Beam();
        BeamCore.pushUi(new CliBeamUI());

        loadPlugins(beam);

        try {
            beam.init(Arrays.asList(arguments));
            beam.run();

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

        String appName = "beam";
        if (System.getProperty("beam.app") != null) {
            File appFile = new File(System.getProperty("beam.app"));
            if (appFile.exists()) {
                appName = appFile.getName();
            }
        }

        Cli.CliBuilder<Object> builder = Cli.<Object>builder(appName)
            .withDescription("Beam.")
            .withDefaultCommand(Help.class)
            .withCommands(commands());

        this.cli = builder.build();
        this.arguments = arguments;
    }

    public Set<Class<?>> commands() {
        return commands;
    }

    public void run() throws IOException {
        try {
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

        } catch (Throwable error) {
            if (error instanceof BeamException) {
                BeamCore.ui().writeError(error.getCause(), "\n@|red Error: %s|@\n", error.getMessage());

            } else {
                BeamCore.ui().writeError(error, "\n@|red Unexpected error! Stack trace follows:|@\n");
            }
        }
    }

    public static void loadPlugins(Beam beam) {
        try {
            // Load ~/.beam/plugins.bcl
            File plugins = Paths.get(getBeamUserHome(), ".beam", "plugins.bcl").toFile();
            if (plugins.exists() && plugins.isFile()) {
                Scope pluginConfig = new LocalStateBackend().load(null, plugins.toString());

                for (PluginLoader loader : pluginConfig.getFileScope().getPluginLoaders()) {
                    for (Class<?> c : loader.classes()) {
                        if (BeamCommand.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                            beam.commands().add(c);
                        }
                    }
                }
            }
        } catch (Throwable error) {
            if (error instanceof BeamException) {
                BeamCore.ui().writeError(error.getCause(), "\n@|red Error: %s|@\n", error.getMessage());

            } else {
                BeamCore.ui().writeError(error, "\n@|red Unexpected error! Stack trace follows:|@\n");
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
