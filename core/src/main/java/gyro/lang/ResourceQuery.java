package gyro.lang;

import java.util.List;
import java.util.Map;

public interface ResourceQuery<T extends Resource> {

    List<T> query(Map<String, String> filter);

    List<T> queryAll();

}
