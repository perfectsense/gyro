/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.cli;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gyro.core.Abort;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.core.backend.LockBackendSettings;
import gyro.core.backend.StateBackendSettings;
import gyro.core.command.AbstractCommand;
import gyro.core.command.GyroCommand;
import gyro.core.command.GyroCommandGroup;
import gyro.core.scope.Defer;
import gyro.core.scope.RootScope;
import gyro.core.validation.ValidationErrorException;
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

public class Gyro {

    private Cli<Object> cli;
    private List<String> arguments;
    private Set<Class<?>> commands = new HashSet<>();

    public static Reflections reflections;

    static {
        Reflections.log = null;
    }

    public static void main(String[] arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        Gyro gyro = new Gyro();
        GyroCore.pushUi(new CliGyroUI());

        try {
            Optional.ofNullable(GyroCore.getRootDirectory())
                .map(d -> new RootScope(GyroCore.INIT_FILE, new LocalFileBackend(d), null, null))
                .ifPresent(r -> {
                    r.load();
                    GyroCore.putStateBackends(r.getSettings(StateBackendSettings.class).getStateBackends());
                    GyroCore.pushLockBackend(r.getSettings(LockBackendSettings.class).getLockBackend());
                });

            gyro.init(Arrays.asList(arguments));
            gyro.run();

        } catch (Abort error) {
            GyroCore.ui().write("\n@|red Aborted!|@\n\n");

        } catch (Throwable error) {
            GyroCore.ui().write("\n");
            writeError(error);
            GyroCore.ui().write("\n");

        } finally {
            GyroCore.popUi();
            GyroCore.popLockBackend();
        }
    }

    private static void writeError(Throwable error) {
        if (error instanceof Defer) {
            ((Defer) error).write(GyroCore.ui());

        } else if (error instanceof GyroException) {
            GyroCore.ui().write("@|red Error:|@ %s\n", error.getMessage());

            Locatable locatable = ((GyroException) error).getLocatable();

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
            List<SyntaxError> errors = s.getErrors();

            GyroCore.ui().write("@|red %d syntax errors in %s!|@\n", errors.size(), s.getFile());

            for (SyntaxError e : errors) {
                GyroCore.ui().write("\n%s %s:\n", e.getMessage(), e.toLocation());
                GyroCore.ui().write("%s", e.toCodeSnippet());
            }

        } else if (error instanceof ValidationErrorException) {
            ((ValidationErrorException) error).write(GyroCore.ui());

        } else {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            error.printStackTrace(pw);

            if (error instanceof Bug) {
                GyroCore.ui()
                    .write(
                        "@|red This should've never happened. Please report this as a bug with the following stack trace:|@ %s\n",
                        sw.toString());

            } else {
                GyroCore.ui().write("@|red Unexpected error:|@ %s\n", sw.toString());
            }
        }
    }

    public void init(List<String> arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        String appName = "gyro";
        if (System.getProperty("gyro.app") != null) {
            File appFile = new File(System.getProperty("gyro.app"));
            if (appFile.exists()) {
                appName = appFile.getName();
            }
        }

        Cli.CliBuilder<Object> builder = Cli.<Object>builder(appName);

        List<Class<?>> groupCommands = new ArrayList<>();
        for (Class<? extends GyroCommandGroup> c : getReflections().getSubTypesOf(GyroCommandGroup.class)) {
            GyroCommandGroup group = gyro.core.Reflections.newInstance(c);
            groupCommands.addAll(group.getCommands());

            builder.withGroup(group.getName())
                .withDescription(group.getDescription())
                .withDefaultCommand(group.getDefaultCommand())
                .withCommands(group.getCommands());
        }

        commands().add(Help.class);
        for (Class<?> c : getReflections().getSubTypesOf(GyroCommand.class)) {
            if (c.isAnnotationPresent(Command.class) && !groupCommands.contains(c)) {
                commands().add(c);
            }
        }

        builder.withDescription("Gyro.")
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

        } else if (command instanceof GyroCommand) {
            if (command instanceof AbstractCommand) {
                ((AbstractCommand) command).setUnparsedArguments(arguments);
            }

            ((GyroCommand) command).execute();

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
