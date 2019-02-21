package gyro.lang.query;

import gyro.lang.Resource;

public abstract class QueryFilter {

    public abstract boolean matches(Resource resource);

}
