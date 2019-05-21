package gyro.core.finder;

import java.util.List;
import java.util.Map;

import gyro.core.auth.Credentials;
import gyro.core.resource.Resource;

public abstract class Finder<R extends Resource> {

    public abstract List<R> findAll(Credentials credentials);

    public abstract List<R> find(Credentials credentials, Map<String, String> filters);

}
