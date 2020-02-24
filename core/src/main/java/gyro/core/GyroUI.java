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

import java.util.Map;

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

    void write(String message, Object... arguments);

    void replace(String message, Object... arguments);

    boolean auditPending();

    void setAuditPending(boolean auditPending);

    void startAuditors(Map<String, Object> log);

    void finishAuditors(Map<String, Object> log, boolean success);

    default <E extends Throwable> void indented(ThrowingProcedure<E> procedure) throws E {
        indent();

        try {
            procedure.execute();

        } finally {
            unindent();
        }
    }
}
