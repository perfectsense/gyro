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

package gyro.core;

import gyro.core.audit.GyroAuditor;
import gyro.core.command.AbstractCommand;

public interface GyroUI {

    boolean isVerbose();

    void setVerbose(boolean verbose);

    boolean readBoolean(Boolean defaultValue, String message, Object... arguments);

    void readEnter(String message, Object... arguments);

    <E extends Enum<E>> E readNamedOption(E options);

    String readPassword(String message, Object... arguments);

    String readText(String message, Object... arguments);

    void indent();

    void unindent();

    String doWrite(String message, Object... arguments);

    String doReplace(String message, Object... arguments);

    default <E extends Throwable> void indented(ThrowingProcedure<E> procedure) throws E {
        indent();

        try {
            procedure.execute();

        } finally {
            unindent();
        }
    }

    default void write(String message, Object... arguments) {
        sendToAuditors(doWrite(message, arguments), false);
    }

    default void replace(String message, Object... arguments) {
        sendToAuditors(doReplace(message, arguments), true);
    }

    static void sendToAuditors(String output, boolean isReplace) {
        AbstractCommand command = AbstractCommand.getCommand();

        if (command != null && command.enableAuditor()) {
            GyroAuditor.AUDITOR_BY_NAME.values().stream()
                .parallel()
                .filter(GyroAuditor::isStarted)
                .filter(auditor -> !auditor.isFinished())
                .forEach(auditor -> {
                    try {
                        auditor.append(output, isReplace);
                    } catch (Exception ex) {
                        // TODO: message
                        System.err.print(ex.getMessage());
                    }
                });
        }
    }
}
