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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gyro.core.Abort;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.LocalFileBackend;
import gyro.core.backend.LockBackendSettings;
import gyro.core.backend.StateBackendSettings;
import gyro.core.command.AbstractCommand;
import gyro.core.command.AbstractDynamicCommand;
import gyro.core.command.GyroCommand;
import gyro.core.command.GyroCommandGroup;
import gyro.core.command.VersionCommand;
import gyro.core.scope.Defer;
import gyro.core.scope.RootScope;
import gyro.core.ui.GyroUINotAvailableException;
import gyro.core.validation.ValidationErrorException;
import gyro.lang.Locatable;
import gyro.lang.SyntaxError;
import gyro.lang.SyntaxErrorException;
import gyro.util.Bug;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "gyro",
    mixinStandardHelpOptions = true,
    commandListHeading = "%nCommands:%n",
    versionProvider = VersionCommand.class
)
public class Gyro {

    private CommandLine commandLine;
    private List<String> arguments;

    public static Reflections reflections;

    static {
        Reflections.log = null;
    }

    public static void main(String[] arguments) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

        GyroCore.pushUi(new CliGyroUI());

        int exitStatus = 0;
        boolean uiOverridden = false;

        try {
            Path rootDirectory = GyroCore.getRootDirectory();

            if (rootDirectory != null) {
                if (Files.exists(rootDirectory.resolve(GyroCore.UI_FILE).normalize())) {
                    new RootScope(GyroCore.UI_FILE, new LocalFileBackend(rootDirectory), null, null).load();
                    uiOverridden = true;
                }

                Optional.of(rootDirectory)
                    .map(d -> new RootScope(GyroCore.INIT_FILE, new LocalFileBackend(d), null, null))
                    .ifPresent(r -> {
                        r.load();
                        GyroCore.putStateBackends(r.getSettings(StateBackendSettings.class).getStateBackends());
                        GyroCore.pushLockBackend(r.getSettings(LockBackendSettings.class).getLockBackend());
                    });
            }

            Gyro gyro = new Gyro();
            gyro.init(Arrays.asList(arguments));
            exitStatus = gyro.run();

        } catch (GyroUINotAvailableException e) {
            exitStatus = 4;
            System.err.print("GyroCloudUI is not available.: ");
            System.err.println(e.getMessage());

        } catch (Abort error) {
            exitStatus = 3;
            GyroCore.ui().write("\n@|red Aborted!|@\n\n");

        } catch (Throwable error) {
            exitStatus = 1;
            GyroCore.ui().write("\n");
            writeError(error, null);
            GyroCore.ui().write("\n");

        } finally {
            if (uiOverridden) {
                GyroCore.popUi();
            }
            GyroCore.popUi();
            GyroCore.popLockBackend();
            System.exit(exitStatus);
        }
    }

    private static void writeError(Throwable error, CommandLine commandLine) {
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
                writeError(cause, null);
            }

            if (commandLine != null && ((GyroException) error).showHelp()) {
                GyroCore.ui().write("\n\n");
                commandLine.usage(commandLine.getOut());
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

        CommandLine commandLine = new CommandLine(this);

        // Add commands part of a GyroCommandGroup
        for (Class<? extends GyroCommandGroup> c : getReflections().getSubTypesOf(GyroCommandGroup.class)) {
            GyroCommandGroup group = gyro.core.Reflections.newInstance(c);
            commandLine.addSubcommand(null, group);
        }

        Set<? extends Class<?>> groupCommands = getSubCommands(commandLine).stream()
            .map(o -> o.getCommandSpec().userObject().getClass())
            .collect(Collectors.toSet());

        // Add all other commands that are not previously added
        for (Class<?> c : getReflections().getSubTypesOf(GyroCommand.class)) {
            if (c.isAnnotationPresent(Command.class) && !groupCommands.contains(c)) {
                Object o = gyro.core.Reflections.newInstance(c);
                commandLine.addSubcommand(null, o);
            }
        }

        // Allow unmatched argument and options for dynamic command usages
        getSubCommands(commandLine).forEach(cmd -> {
            if (cmd.getCommandSpec().userObject() instanceof AbstractDynamicCommand) {
                cmd.setUnmatchedArgumentsAllowed(true);
                cmd.setUnmatchedOptionsAllowedAsOptionParameters(true);
                cmd.setUnmatchedOptionsArePositionalParams(true);
            }
        });

        commandLine.setParameterExceptionHandler(Gyro::handleParseException);
        commandLine.setExecutionExceptionHandler(Gyro::invalidUserInput);

        this.commandLine = commandLine;

        this.arguments = arguments;
    }

    private Set<CommandLine> getSubCommands(CommandLine commandLine) {
        Set<CommandLine> commandLines = new HashSet<>();
        commandLines.add(commandLine);
        Set<CommandLine> subCommandLines = new HashSet<>(commandLine.getSubcommands().values());

        for (CommandLine subCommandLine : subCommandLines) {
            commandLines.addAll(getSubCommands(subCommandLine));
        }

        return commandLines;
    }

    public static int handleParseException(CommandLine.ParameterException ex, String[] args) {
        String message = ex.getMessage();

        if (message.startsWith("Unmatched argument")) {
            message = "Unrecognized command: " + message.split(":")[1];
        } else if (message.startsWith("Unknown option")) {
            message = "Unrecognized option: " + message.split(":")[1];
        }

        GyroCore.ui().write(String.format("\n@|red %s|@\n\n", message));

        CommandLine commandLine = ex.getCommandLine();
        commandLine.usage(commandLine.getOut());
        return 1;
    }

    // custom handler for invalid input
    private static int invalidUserInput(Exception error, CommandLine commandLine, CommandLine.ParseResult parseResult) {
        writeError(error, commandLine);
        return 2;
    }

    public int run() {

        try {
            CommandLine.ParseResult parseResult = commandLine.parseArgs(arguments.toArray(new String[0]));

            if (parseResult.hasSubcommand()) {
                CommandLine.ParseResult subcommand = parseResult.subcommand();
                Object commandObject = subcommand.commandSpec().userObject();
                if (commandObject instanceof AbstractCommand) {
                    ((AbstractCommand) commandObject).setUnparsedArguments(arguments);
                } else if (commandObject instanceof AbstractDynamicCommand) {
                    ((AbstractDynamicCommand) commandObject).setParseResult(parseResult);
                }
            }
        } catch (CommandLine.UnmatchedArgumentException | CommandLine.MissingParameterException ex) {
            // Ignore
            // The execute will handle throwing the error
        }

        return commandLine.execute(this.arguments.toArray(new String[0]));
    }

    public static Reflections getReflections() {
        if (reflections == null) {
            reflections = new Reflections(new org.reflections.util.ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("gyro")));
        }

        return reflections;
    }
}
