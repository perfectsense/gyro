package gyro.core.resource;

import java.util.List;
import java.util.Map;

import gyro.core.Credentials;

public interface ResourceFinder<R extends Resource> {

    List<R> findAll(Credentials credentials);

    List<R> find(Credentials credentials, Map<String, String> filters);
}
