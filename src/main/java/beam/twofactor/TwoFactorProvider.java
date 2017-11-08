package beam.twofactor;

import java.util.Map;

public abstract class TwoFactorProvider {

    private String sessionId;

    private boolean success = false;

    public abstract void handleNeeds2FA(Map<String, Object> authMap);

    public abstract void handleNeeds2FAEnrollment(Map<String, Object> authMap);

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
