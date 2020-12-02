/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.audit;

import java.util.HashMap;
import java.util.Map;

import gyro.core.GyroException;
import gyro.core.GyroUI;

public abstract class GyroAuditableUI implements GyroUI {

    // TODO: pending replaces?
    private final StringBuilder pendingWrites = new StringBuilder();

    private boolean auditPending;

    public abstract String doWrite(String message, Object... arguments);

    public abstract String doReplace(String message, Object... arguments);

    public void finishAuditors() {
        finishAuditors(null, false);
    }

    @Override
    public boolean auditPending() {
        return auditPending;
    }

    @Override
    public void setAuditPending(boolean auditPending) {
        this.auditPending = auditPending;
    }

    @Override
    public void write(String message, Object... arguments) {
        String output = doWrite(message, arguments);

        if (auditPending()) {
            pendingWrites.append(output);
        } else {
            sendToAuditors(output, false);
        }

    }

    @Override
    public void replace(String message, Object... arguments) {
        sendToAuditors(doReplace(message, arguments), true);
    }

    @Override
    public void startAuditors(Map<String, Object> log) {
        GyroAuditor.AUDITOR_BY_NAME.values().stream()
            .parallel()
            .filter(auditor -> !auditor.isStarted())
            .forEach(auditor -> {
                try {
                    auditor.start(log);
                } catch (Exception ex) {
                    throw new GyroException(ex.getMessage());
                }
            });
        Runtime.getRuntime().addShutdownHook(new Thread(this::finishAuditors));
        flushPendingWrites();
    }

    @Override
    public void finishAuditors(Map<String, Object> log, boolean success) {
        if (log == null) {
            log = new HashMap<>();
        }
        //log.putAll(MetadataDirectiveProcessor.getMetadata());
        Map<String, Object> finalLog = log;

        GyroAuditor.AUDITOR_BY_NAME.values().stream()
            .parallel()
            .filter(GyroAuditor::isStarted)
            .filter(auditor -> !auditor.isFinished())
            .forEach(auditor -> {
                try {
                    auditor.finish(finalLog, success);
                } catch (Exception ex) {
                    throw new GyroException(ex.getMessage());
                }
            });
    }

    private void sendToAuditors(String output, boolean isReplace) {
        GyroAuditor.AUDITOR_BY_NAME.values().stream()
            .parallel()
            .filter(GyroAuditor::isStarted)
            .filter(auditor -> !auditor.isFinished())
            .forEach(auditor -> {
                try {
                    auditor.append(output, isReplace);
                } catch (Exception ex) {
                    throw new GyroException(ex.getMessage());
                }
            });
    }

    private void flushPendingWrites() {
        if (pendingWrites.length() > 0) {
            sendToAuditors(pendingWrites.toString(), false);
            pendingWrites.setLength(0);
            setAuditPending(false);
        }
    }
}
