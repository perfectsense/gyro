package beam.enterprise;

import beam.BeamAuditor;
import com.psddev.dari.util.ObjectUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnterpriseAuditor implements BeamAuditor {

    private UUID auditKey;

    private final StringBuilder buffer = new StringBuilder();

    private EnterpriseAuditThread auditThread;

    private Long MAX_LENGTH = 10485760L;

    public void start(Map<String, Object> log) throws IOException {
        if (EnterpriseApi.isAvailable()) {
            Map<String, Object> addMap = EnterpriseApi.call(
                    "audit/start",
                    new BasicNameValuePair("accountName", ObjectUtils.to(String.class, log.get("accountName"))),
                    new BasicNameValuePair("projectName", ObjectUtils.to(String.class, log.get("projectName"))),
                    new BasicNameValuePair("log", ObjectUtils.toJson(log)));

            if (!"ok".equals(addMap.get("status"))) {
                throw new EnterpriseException(addMap);
            }

            auditKey = ObjectUtils.to(UUID.class, addMap.get("auditKey"));
            auditThread = new EnterpriseAuditThread(auditKey, buffer);
            auditThread.start();
        }
    }

    public void append(String output) {
        if (EnterpriseApi.isAvailable()) {
            synchronized (EnterpriseAuditor.class) {
                if (buffer.length() < MAX_LENGTH) {
                    buffer.append(output);
                }
            }
        }
    }

    public void finish(Map<String, Object> log, boolean success) throws IOException {
        if (EnterpriseApi.isAvailable() && auditThread != null) {
            try {
                auditThread.setKeepAuditing(false);
                auditThread.interrupt();
                auditThread.join();
            } catch (InterruptedException ie) {

            }

            Map<String, Object> addMap = EnterpriseApi.call("audit/finish",
                    new BasicNameValuePair("auditKey", ObjectUtils.to(String.class, auditKey)),
                    new BasicNameValuePair("success", ObjectUtils.to(String.class, success)));

            if (!"ok".equals(addMap.get("status"))) {
                throw new EnterpriseException(addMap);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list(String accountName, String projectName) throws IOException {
        if (EnterpriseApi.isAvailable()) {
            return (List<Map<String, Object>>) EnterpriseApi.call(
                    "audit/list",
                    new BasicNameValuePair("accountName", accountName),
                    new BasicNameValuePair("projectName", projectName)).
                    get("logs");

        } else {
            return null;
        }
    }

    public static class EnterpriseAuditThread extends Thread {

        private UUID auditKey;

        private final StringBuilder buffer;

        private boolean keepAuditing;

        public EnterpriseAuditThread(UUID auditKey, StringBuilder buffer) {
            this.auditKey = auditKey;
            this.buffer = buffer;
            this.keepAuditing = true;
        }

        public boolean isKeepAuditing() {
            return keepAuditing;
        }

        public void setKeepAuditing(boolean keepAuditing) {
            this.keepAuditing = keepAuditing;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Enterprise Audit Thread");

            while (isKeepAuditing()) {
                sendLogs();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                   setKeepAuditing(false);
                }
            }

            sendLogs();
        }

        private void sendLogs() {
            String log = null;
            synchronized (EnterpriseAuditor.class) {
                log = buffer.toString();
                buffer.setLength(0);
            }

            Logger LOGGER = ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));
            Level level = LOGGER.getLevel();

            LOGGER.setLevel(Level.OFF);
            try {
                if (!ObjectUtils.isBlank(log)) {
                    EnterpriseApi.call("audit/append",
                            new BasicNameValuePair("auditKey", ObjectUtils.to(String.class, auditKey)),
                            new BasicNameValuePair("log", log));
                }
            } catch (Exception ioe) {
            }

            LOGGER.setLevel(level);
        }
    }
}
