package beam.lang.ast.scope;

import java.util.HashMap;
import java.util.Map;

import beam.lang.Credentials;

public class RootScope extends FileScope {

    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Credentials> credentialsMap = new HashMap<>();

    public RootScope(String file) {
        super(null, file);
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public Map<String, Credentials> getCredentialsMap() {
        return credentialsMap;
    }

}
