package beam.lang.ast.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beam.lang.Credentials;

public class RootScope extends FileScope {

    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final List<Credentials> credentialsList = new ArrayList<>();

    public RootScope(String file) {
        super(null, file);
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public List<Credentials> getCredentialsList() {
        return credentialsList;
    }

}
