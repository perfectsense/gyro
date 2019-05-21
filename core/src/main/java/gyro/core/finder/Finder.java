package gyro.core.finder;

import java.util.List;
import java.util.Map;

import com.psddev.dari.util.TypeDefinition;
import gyro.core.auth.Credentials;
import gyro.core.resource.DiffableScope;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.resource.Scope;

public abstract class Finder<R extends Resource> {

    Scope scope;

    public abstract List<R> findAll();

    public abstract List<R> find(Map<String, String> filters);

    public Credentials credentials() {
        return Credentials.getInstance(getClass(), scope);
    }

    public R newResource() {
        @SuppressWarnings("unchecked")
        Class<R> resourceClass = (Class<R>) TypeDefinition.getInstance(getClass())
            .getInferredGenericTypeArgumentClass(Finder.class, 0);

        return DiffableType.getInstance(resourceClass)
            .newDiffable(null, null, new DiffableScope(scope));
    }

}
