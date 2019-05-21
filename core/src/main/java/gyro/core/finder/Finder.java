package gyro.core.finder;

import java.util.List;
import java.util.Map;

import gyro.core.auth.Credentials;
import gyro.core.resource.Resource;
import gyro.core.resource.Scope;

public abstract class Finder<R extends Resource> {

    Scope scope;

    public abstract List<R> findAll();

    public abstract List<R> find(Map<String, String> filters);

    public Credentials<?> credentials() {
        return Credentials.getInstance(getClass(), scope);
    }

}
