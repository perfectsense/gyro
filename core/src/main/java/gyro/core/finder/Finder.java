package gyro.core.finder;

import java.util.List;
import java.util.Map;

import gyro.core.auth.Credentials;
import gyro.core.resource.Resource;

public interface Finder<R extends Resource> {

    List<R> findAll(Credentials credentials);

    List<R> find(Credentials credentials, Map<String, String> filters);
}
