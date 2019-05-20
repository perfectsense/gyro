package gyro.core.auth;

import java.util.HashMap;
import java.util.Map;

import gyro.core.resource.Settings;

public class CredentialsSettings extends Settings {

    private Map<String, Class<? extends Credentials<?>>> credentialsClasses;
    private Map<String, Credentials<?>> credentialsByName;

    public Map<String, Class<? extends Credentials<?>>> getCredentialsClasses() {
        if (credentialsClasses == null) {
            credentialsClasses = new HashMap<>();
        }

        return credentialsClasses;
    }

    public void setCredentialsClasses(Map<String, Class<? extends Credentials<?>>> credentialsClasses) {
        this.credentialsClasses = credentialsClasses;
    }

    public Map<String, Credentials<?>> getCredentialsByName() {
        if (credentialsByName == null) {
            credentialsByName = new HashMap<>();
        }

        return credentialsByName;
    }

    public void setCredentialsByName(Map<String, Credentials<?>> credentialsByName) {
        this.credentialsByName = credentialsByName;
    }

}
