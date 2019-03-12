package gyro.lang;

import java.util.List;
import java.util.Map;

public interface ResourceFinder<R extends Resource> {

    List<R> findAll(Credentials credentials);

    List<R> find(Credentials credentials, Map<String, String> filters);
}
