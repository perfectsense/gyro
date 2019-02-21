package gyro.lang.query;

import gyro.lang.Resource;

import java.util.List;

public interface ResourceQuery<T extends Resource> {

    List<T> query(List<QueryFilter> filters);

}
