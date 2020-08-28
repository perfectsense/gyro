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
import java.util.concurrent.Callable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gyro.core.GyroCore;
import gyro.core.GyroUI;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

/**
 * Basic {@link GyroCommand} implementation that adds the global {@code -debug} option for more detailed logging.
 *
 * <p>Subclasses must override:</p>
 *
 * <ul>
 * <li>{@link #doExecute()}</li>
 * </ul>
 */
public abstract class AbstractCommand implements GyroCommand, Callable<Integer> {

    @Option(names = "--debug", description = "Output cloud provider API request/response log data. Warning: Output may contain sensitive information.")
    public boolean debug;

    @Option(names = "--verbose", description = "Show values of attributes that changed.")
    private boolean verbose;

    private List<String> unparsedArguments;

    protected abstract void doExecute() throws Exception;

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

        doExecute();
    }

    @Override
    public Integer call() throws Exception {
        execute();
        return 0;
    }

    public boolean isDebug() {
        return debug;
    }

    protected void startAuditors(GyroUI ui) {
        Map<String, Object> log = new HashMap<>();
        log.put("commandArguments", getUnparsedArguments());

        try {
            log.put("version", VersionCommand.getCurrentVersion().toString());
        } catch (IOException e) {
            // Do nothing.
        }

        ui.startAuditors(log);
    }
}
