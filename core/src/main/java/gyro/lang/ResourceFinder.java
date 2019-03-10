package gyro.lang;

import gyro.lang.ast.scope.Scope;

import java.util.List;
import java.util.Map;

public abstract class ResourceFinder<R extends Resource> {

    private Credentials credentials;

    public Credentials credentials() {
        return credentials;
    }

    public void credentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Credentials resourceCredentials(Scope scope) {

        scope = scope.getRootScope();

        if (scope != null) {
            String name = (String) scope.get("resource-credentials");

            if (name == null) {
                name = "default";
            }

            for (Resource resource : scope.getRootScope().findAllResources()) {
                if (resource instanceof Credentials) {
                    Credentials credentials = (Credentials) resource;

                    if (credentials.resourceIdentifier().equals(name)) {
                        return credentials;
                    }
                }
            }
        }

        throw new IllegalStateException();
    }

    public abstract List<R> findAll();

    public abstract List<R> find(Map<String, String> filters);
}
