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

package gyro.core.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.audit.GyroAuditor;
import gyro.core.audit.MetadataDirectiveProcessor;
import gyro.core.workflow.Workflow;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.slf4j.LoggerFactory;

/**
 * Basic {@link GyroCommand} implementation that adds the global {@code -debug} option for more detailed logging.
 *
 * <p>Subclasses must override:</p>
 *
 * <ul>
 * <li>{@link #doExecute()}</li>
 * <li>{@link #enableAuditor()}</li>
 * </ul>
 */
public abstract class AbstractCommand implements GyroCommand {

    private static boolean success = false;

    private static AbstractCommand currentCommand;

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public boolean debug;

    @Option(name = "--verbose")
    private boolean verbose;

    private List<String> unparsedArguments;

    protected abstract void doExecute() throws Exception;

    public abstract boolean enableAuditor();

    public static AbstractCommand getCurrentCommand() {
        return currentCommand;
    }

    public List<String> getUnparsedArguments() {
        return unparsedArguments;
    }

    public void setUnparsedArguments(List<String> unparsedArguments) {
        this.unparsedArguments = unparsedArguments;
    }

    @Override
    public void execute() throws Exception {
        GyroCore.ui().setVerbose(verbose);

        if (debug || "debug".equalsIgnoreCase(System.getenv("GYRO_LOG"))) {
            System.getProperties().setProperty("org.openstack4j.core.transport.internal.HttpLoggingFilter", "true");

            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
        } else {
            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);

            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        }

        currentCommand = this;

        if (enableAuditor()) {
            startAuditors();
        }

        doExecute();

        success = true;
    }

    public boolean isDebug() {
        return debug;
    }

    private void startAuditors() {
        Map<String, Object> log = new HashMap<>();
        log.put("commandArguments", getUnparsedArguments());

        try {
            log.put("version", VersionCommand.getCurrentVersion().toString());
        } catch (IOException e) {
            // Do nothing.
        }

        GyroAuditor.AUDITOR_BY_NAME.entrySet().stream()
            .parallel()
            .filter(e -> !e.getValue().isStarted())
            .forEach(e -> {
                try {
                    e.getValue().start(log);
                } catch (Exception ex) {
                    throw new GyroException(ex.getMessage());
                }
            });
        Runtime.getRuntime().addShutdownHook(new Thread(this::finishAuditors));
    }

    private void finishAuditors() {
        Map<String, Object> log = MetadataDirectiveProcessor.getMetadata(Workflow.getSuccessfullyExecutedWorkflows());

        GyroAuditor.AUDITOR_BY_NAME.entrySet().stream()
            .parallel()
            .filter(e -> e.getValue().isStarted())
            .filter(e -> !e.getValue().isFinished())
            .forEach(e -> {
                try {
                    e.getValue().finish(log, success);
                } catch (Exception ex) {
                    throw new GyroException(ex.getMessage());
                }
            });
    }
}
