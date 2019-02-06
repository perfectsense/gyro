package beam.test;

import java.util.HashMap;
import java.util.Map;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;

public class AuthenticateCognitoConfig extends Diffable {

    private Map<String, String> extraParams;
    private String onUnauthenticatedRequest;
    private String scope;
    private String sessionCookieName;
    private Long sessionTimeout;
    private String userPoolArn;
    private String userPoolClientId;
    private String userPoolDomain;

    public Map<String, String> getExtraParams() {
        if (extraParams == null) {
            extraParams = new HashMap<>();
        }

        return extraParams;
    }

    public void setExtraParams(Map<String, String> extraParams) {
        this.extraParams = extraParams;
    }

    public String getOnUnauthenticatedRequest() {
        return onUnauthenticatedRequest;
    }

    public void setOnUnauthenticatedRequest(String onUnauthenticatedRequest) {
        this.onUnauthenticatedRequest = onUnauthenticatedRequest;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public void setSessionCookieName(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getUserPoolArn() {
        return userPoolArn;
    }

    public void setUserPoolArn(String userPoolArn) {
        this.userPoolArn = userPoolArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getUserPoolClientId() {
        return userPoolClientId;
    }

    public void setUserPoolClientId(String userPoolClientId) {
        this.userPoolClientId = userPoolClientId;
    }

    public String getUserPoolDomain() {
        return userPoolDomain;
    }

    public void setUserPoolDomain(String userPoolDomain) {
        this.userPoolDomain = userPoolDomain;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("cognito ");
        sb.append(getUserPoolClientId());

        return sb.toString();
    }

}